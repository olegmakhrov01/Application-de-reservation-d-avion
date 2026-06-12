const cron = require("node-cron");
const express = require("express");
const cors = require("cors");
const { Pool } = require("pg");

const app = express();

// ✅ Middleware
app.use(cors());
app.use(express.json());

const db = new Pool({
  connectionString: process.env.DATABASE_URL,
  ssl: { rejectUnauthorized: false },
  
  // 💡 MANDATORY FOR RENDER INTERNAL ROUTING:
  max: 2,                             // Keep the pool tiny so Render doesn't kick you out
  idleTimeoutMillis: 1000,            // Instantly drop idle sockets before Render kills them
  connectionTimeoutMillis: 5000,      // Allow ample time for the secure handshake
  maxUses: 1                          // Fresh connection per request to avoid session decay
});

// Prevents the pool from crashing the Node process if an idle socket drops
db.on('error', (err) => {
  console.error('⚠️ Database pool connection drop:', err.message);
});

// Ajoute ce log pour voir ce que l'app lit VRAIMENT
console.log("DEBUG: Connection string being used:", process.env.DATABASE_URL ? "URL PRESENT" : "URL MISSING");

// ✅ GET all users
app.get("/users", async (req, res) => {
  try {
    const result = await db.query("SELECT * FROM users");
    res.json(result.rows);
  } catch (err) {
    console.error("❌ Error fetching users:", err);
    res.status(500).json({ error: "Server error" });
  }
});

// ✅ POST create new user
app.post("/users", async (req, res) => {
    const { nom, prenom, email, mot_de_passe } = req.body;
  
    if (!nom || !prenom || !email || !mot_de_passe) {
      return res.status(400).json({ error: "Champs manquants" });
    }
  
    try {
      const result = await db.query(
        `INSERT INTO users (nom, prenom, email, mot_de_passe, role)
         VALUES ($1, $2, $3, $4, 'user') RETURNING id`,
        [nom, prenom, email, mot_de_passe]
      );
  
      res.status(201).json({ message: "✅ Utilisateur créé avec succès", userId: result.rows[0].id });
  
    } catch (err) {
      // 💡 Handle duplicate email error (PostgreSQL error code 23505)
      if (err.code === '23505') {
        return res.status(400).json({ error: "Cet email est déjà utilisé." });
      }
  
      console.error("❌ SQL Insert Error:", err);
      res.status(500).json({ error: "Erreur lors de l'enregistrement" });
    }
});

// ✅ DELETE user
app.delete("/users/:id", async (req, res) => {
    const { id } = req.params;
  
    try {
      // 🔁 Récupérer tous les vols que l'utilisateur avait réservés
      const resVols = await db.query(
        "SELECT id_vol FROM reservations WHERE id_utilisateur = $1",
        [id]
      );
  
      // 🔁 Pour chaque vol, incrémenter places_restantes
      for (const row of resVols.rows) {
        await restorePlace(row.id_vol); // ✅ ta fonction existante !
      }
  
      // ✅ Supprimer les réservations (les places ont été remises avant)
      await db.query("DELETE FROM reservations WHERE id_utilisateur = $1", [id]);
  
      // ✅ Supprimer les notifications
      await db.query("DELETE FROM notifications WHERE id_utilisateur = $1", [id]);
  
      // ✅ Supprimer l'utilisateur
      const result = await db.query("DELETE FROM users WHERE id = $1", [id]);
  
      if (result.rowCount === 0) {
        return res.status(404).json({ error: "Utilisateur non trouvé" });
      }
  
      res.json({ message: "✅ Compte supprimé avec succès" });
  
    } catch (err) {
      console.error("❌ Erreur SQL suppression utilisateur:", err);
      res.status(500).json({ error: "Erreur serveur" });
    }
  });
  

// ✅ GET vols with filters
app.get("/vols", async (req, res) => {
  let sql = "SELECT * FROM vols WHERE 1=1";
  const params = [];
  let i = 1;
  if (req.query.destination) {
    sql += ` AND destination = $${i++}`;
    params.push(req.query.destination);
  }
  if (req.query.date) {
    sql += ` AND date = $${i++}`;
    params.push(req.query.date);
  }
  if (req.query.prix) {
    sql += ` AND prix <= $${i++}`;
    params.push(req.query.prix);
  }
  try {
    const result = await db.query(sql, params);
    res.json(result.rows);
  } catch (err) {
    console.error("Erreur SQL:", err);
    res.status(500).json({ error: "Erreur serveur" });
  }
});

// ✅ GET vol + seats
app.get("/vols/:id", async (req, res) => {
  try {
    const volId = req.params.id;
    const volRes = await db.query("SELECT * FROM vols WHERE id = $1", [volId]);
    if (volRes.rows.length === 0) return res.status(404).json({ error: "Vol non trouvé" });

    const reserved = await db.query("SELECT siege FROM reservations WHERE id_vol = $1", [volId]);
    const reservedSeats = reserved.rows.map(r => parseInt(r.siege));
    const totalSeats = volRes.rows[0].places_restantes || 100;
    const availableSeats = Array.from({ length: totalSeats }, (_, i) => i + 1).filter(s => !reservedSeats.includes(s));

    const vol = volRes.rows[0];
    vol.places_disponibles = availableSeats;
    res.json(vol);
  } catch (err) {
    console.error("❌ Error getting vol:", err);
    res.status(500).json({ error: "Erreur SQL" });
  }
});

// ✅ POST reserver
app.post("/reserver", async (req, res) => {
    const { id_utilisateur, id_vol, siege, paiement } = req.body;
  
    if (!id_utilisateur || !id_vol || !siege || !paiement) {
      return res.status(400).json({ error: "Champs requis manquants" });
    }
  
    try {
      const check = await db.query(
        "SELECT COUNT(*) FROM reservations WHERE id_vol = $1 AND siege = $2",
        [id_vol, siege]
      );
  
      if (parseInt(check.rows[0].count) > 0) {
        return res.status(400).json({ error: "Ce siège est déjà réservé" });
      }
  
      const result = await db.query(
        "INSERT INTO reservations (id_utilisateur, id_vol, siege) VALUES ($1, $2, $3) RETURNING id",
        [id_utilisateur, id_vol, siege]
      );
  
      await updatePlacesRestantes(id_vol);
  
      // ✅ Ajouter une notification
      const volInfo = await db.query("SELECT destination, date FROM vols WHERE id = $1", [id_vol]);
      if (volInfo.rows.length > 0) {
        const { destination, date } = volInfo.rows[0];
        const contenu = `Réservation confirmée pour votre vol vers ${destination} le ${new Date(date).toLocaleDateString()}`;
        await db.query(
          "INSERT INTO notifications (id_utilisateur, contenu) VALUES ($1, $2)",
          [id_utilisateur, contenu]
        );
      }
  
      res.status(201).json({
        message: "Réservation confirmée ✅",
        reservationId: result.rows[0].id
      });
    } catch (err) {
      console.error("Erreur SQL insert réservation:", err);
      res.status(500).json({ error: "Erreur de réservation" });
    }
  });

// ✅ GET reservations par utilisateur
app.get("/reservations/:userId", async (req, res) => {
  try {
    const { userId } = req.params;
    const result = await db.query(
      `SELECT reservations.id AS id_reservation, reservations.id_vol, reservations.siege,
              vols.date, vols.depart, vols.destination, vols.prix
       FROM reservations
       JOIN vols ON reservations.id_vol = vols.id
       WHERE reservations.id_utilisateur = $1`,
      [userId]
    );
    res.json(result.rows);
  } catch (err) {
    console.error("❌ Erreur SQL reservations:", err);
    res.status(500).json({ error: "Erreur serveur" });
  }
});

// ✅ Cancel reservation
app.post("/cancel_reservation", async (req, res) => {
  const { id_reservation, id_vol } = req.body;
  if (!id_reservation || !id_vol) {
    return res.status(400).json({ message: "Champs manquants" });
  }
  try {
    const del = await db.query("DELETE FROM reservations WHERE id = $1", [id_reservation]);
    if (del.rowCount > 0) {
      await restorePlace(id_vol);
      return res.status(200).json({ message: "Réservation annulée avec succès" });
    } else {
      return res.status(404).json({ message: "Réservation non trouvée" });
    }
  } catch (err) {
    console.error("❌ Erreur SQL cancel:", err);
    res.status(500).json({ error: "Erreur serveur" });
  }
});

// ✅ GET user info
app.get("/users/:id", async (req, res) => {
  try {
    const { id } = req.params;
    const result = await db.query("SELECT nom, prenom, email FROM users WHERE id = $1", [id]);
    if (result.rows.length === 0) {
      return res.status(404).json({ error: "Utilisateur non trouvé" });
    }
    res.json(result.rows[0]);
  } catch (err) {
    console.error("❌ Erreur SQL get user:", err);
    res.status(500).json({ error: "Erreur serveur" });
  }
});

// ✅ PUT update user
app.put("/users/:id", async (req, res) => {
  const { id } = req.params;
  const { email, mot_de_passe, nom, prenom } = req.body;

  const fields = [];
  const values = [];
  let i = 1;

  if (email) { fields.push(`email = $${i++}`); values.push(email); }
  if (mot_de_passe) { fields.push(`mot_de_passe = $${i++}`); values.push(mot_de_passe); }
  if (nom) { fields.push(`nom = $${i++}`); values.push(nom); }
  if (prenom) { fields.push(`prenom = $${i++}`); values.push(prenom); }

  if (fields.length === 0) return res.status(400).json({ error: "Aucun champ à mettre à jour" });

  try {
    const result = await db.query(`UPDATE users SET ${fields.join(", ")} WHERE id = $${i}`, [...values, id]);
    if (result.rowCount === 0) return res.status(404).json({ error: "Utilisateur non trouvé" });
    res.json({ message: "✅ Compte mis à jour avec succès" });
  } catch (err) {
    console.error("❌ Erreur SQL update user:", err);
    res.status(500).json({ error: "Erreur serveur" });
  }
});

// ✅ POST notification
app.post("/notifications", async (req, res) => {
    const { id_utilisateur, notification } = req.body;
  
    if (!id_utilisateur || !notification) {
      return res.status(400).json({ error: "Champs manquants" });
    }
  
    try {
      const result = await db.query(
        `INSERT INTO notifications (id_utilisateur, contenu)
         VALUES ($1, $2) RETURNING id`,
        [id_utilisateur, notification]
      );
  
      res.status(201).json({ message: "✅ Notification enregistrée", id: result.rows[0].id });
  
    } catch (err) {
      console.error("❌ Erreur SQL insertion notification:", err);
      res.status(500).json({ error: "Erreur lors de l'enregistrement de la notification" });
    }
  });

  // ✅ GET notifications pour un utilisateur
app.get("/notifications/:userId", async (req, res) => {
    const { userId } = req.params;
  
    try {
      const result = await db.query(
        "SELECT id, contenu FROM notifications WHERE id_utilisateur = $1 ORDER BY date_notification DESC",
        [userId]
      );
      res.json(result.rows); // renvoie un tableau de { contenu: "..." }
    } catch (err) {
      console.error("❌ Erreur SQL notifications:", err);
      res.status(500).json({ error: "Erreur serveur" });
    }
  });
  

  app.delete("/notifications/:userId", async (req, res) => {
    const { userId } = req.params;
    try {
      await db.query("DELETE FROM notifications WHERE id_utilisateur = $1", [userId]);
      res.json({ message: "✅ Notifications supprimées" });
    } catch (err) {
      console.error(err);
      res.status(500).json({ error: "Erreur serveur" });
    }
  });

  app.delete("/notifications/delete/:notificationId", async (req, res) => {
    const { notificationId } = req.params;
    try {
      const result = await db.query("DELETE FROM notifications WHERE id = $1", [notificationId]);
      if (result.rowCount === 0) {
        return res.status(404).json({ error: "Notification non trouvée" });
      }
      res.json({ message: "✅ Notification supprimée" });
    } catch (err) {
      console.error("❌ Erreur suppression notification:", err);
      res.status(500).json({ error: "Erreur serveur" });
    }
  });
  
  
  

// ✅ Helper: Mise à jour des places restantes
async function updatePlacesRestantes(id_vol) {
  try {
    await db.query("UPDATE vols SET places_restantes = places_restantes - 1 WHERE id = $1 AND places_restantes > 0", [id_vol]);
    console.log("✅ Places restantes mises à jour.");
  } catch (err) {
    console.error("❌ Erreur SQL mise à jour places restantes:", err);
  }
}

// ✅ Helper: Restaurer une place
async function restorePlace(id_vol) {
  try {
    await db.query("UPDATE vols SET places_restantes = places_restantes + 1 WHERE id = $1", [id_vol]);
    console.log("✅ Place restaurée pour le vol", id_vol);
  } catch (err) {
    console.error("❌ Erreur SQL restore place:", err);
  }
}

// ✅ Serveur
app.get("/", (req, res) => {
  res.send("✅ Backend Render OK");
});

const PORT = process.env.PORT || 10000;
app.listen(PORT, () => console.log(`🚀 API running on port ${PORT}`));
