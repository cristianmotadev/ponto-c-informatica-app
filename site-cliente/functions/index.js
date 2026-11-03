const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();

async function exigirDono(auth) {
  if (!auth || !auth.uid) {
    throw new HttpsError("unauthenticated", "Faça login novamente.");
  }
  const snap = await admin.firestore().collection("usuarios").doc(auth.uid).get();
  if (!snap.exists || snap.data().papel !== "dono") {
    throw new HttpsError("permission-denied", "Ação exclusiva do dono.");
  }
}

exports.redefinirSenha = onCall(async (request) => {
  await exigirDono(request.auth);
  const uid = request.data && request.data.uid;
  if (!uid) throw new HttpsError("invalid-argument", "uid obrigatório.");
  if (uid === request.auth.uid) {
    throw new HttpsError("failed-precondition", "Use 'Meu perfil' para trocar a sua própria senha.");
  }

  const temp = Math.random().toString(36).slice(2, 10) + "A1!";
  await admin.auth().updateUser(uid, { password: temp });

  // FASE 12: obriga o funcionário a trocar no próximo login
  await admin.firestore().collection("usuarios").doc(uid).update({
    senhaTemporaria: true
  });

  return { senhaTemporaria: temp };
});

exports.excluirUsuario = onCall(async (request) => {
  await exigirDono(request.auth);
  const uid = request.data && request.data.uid;
  if (!uid) throw new HttpsError("invalid-argument", "uid obrigatório.");
  if (uid === request.auth.uid) {
    throw new HttpsError("failed-precondition", "O dono não pode excluir a si mesmo.");
  }

  const db = admin.firestore();
  const doc = await db.collection("usuarios").doc(uid).get();

  await admin.auth().deleteUser(uid).catch(() => null);

  if (doc.exists && doc.data().username) {
    await db.collection("usuariosPorNome").doc(doc.data().username).delete().catch(() => {});
  }
  if (doc.exists) {
    await db.collection("usuarios").doc(uid).delete();
  }
  return { ok: true };
});