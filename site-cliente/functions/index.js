// FASE 11: callables administrativos — somente o dono pode chamar.
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const admin = require("firebase-admin");

admin.initializeApp();

// Garante que quem chama é o dono (lê o documento de usuário no Firestore)
async function exigirDono(auth) {
  if (!auth) throw new HttpsError("unauthenticated", "Não autenticado.");
  const snap = await admin.firestore().collection("usuarios").doc(auth.uid).get();
  if (!snap.exists || snap.data().papel !== "dono") {
    throw new HttpsError("permission-denied", "Somente o dono pode executar esta ação.");
  }
}

// Exclusão real: Auth + perfil + índice de username.
// IDEMPOTENTE: se uma das partes não existir (conta "zumbi"),
// ignora e continua limpando o resto — nunca aborta no meio.
exports.excluirUsuario = onCall(async (request) => {
  await exigirDono(request.auth);
  const uid = request.data && request.data.uid;
  if (!uid || typeof uid !== "string") {
    throw new HttpsError("invalid-argument", "UID inválido.");
  }
  if (uid === request.auth.uid) {
    throw new HttpsError("invalid-argument", "O dono não pode excluir a si mesmo.");
  }

  // 1) Auth: tolera "usuário não existe" (zumbis: só Firestore sobrou)
  try {
    await admin.auth().deleteUser(uid);
  } catch (e) {
    if (e.code !== "auth/user-not-found") throw e;
  }

  // 2) Firestore: perfil + índice de username (se houver)
  const alvo = await admin.firestore().collection("usuarios").doc(uid).get();
  const username = alvo.exists ? (alvo.data().username || "") : "";
  await admin.firestore().collection("usuarios").doc(uid).delete();
  if (username) {
    await admin.firestore()
      .collection("usuariosPorNome").doc(username).delete()
      .catch(() => {});
  }
  return { ok: true };
});

// Redefinição de senha pelo dono (senha temporária gerada no app)
exports.redefinirSenha = onCall(async (request) => {
  await exigirDono(request.auth);
  const uid = request.data && request.data.uid;
  const novaSenha = request.data && request.data.novaSenha;
  if (uid === request.auth.uid) {
    throw new HttpsError("invalid-argument", "Troque a própria senha pelo dispositivo.");
  }
  if (!novaSenha || novaSenha.length < 6) {
    throw new HttpsError("invalid-argument", "Senha mínima: 6 caracteres.");
  }
  try {
    await admin.auth().updateUser(uid, { password: novaSenha });
  } catch (e) {
    if (e.code === "auth/user-not-found") {
      throw new HttpsError(
        "not-found",
        "Este perfil não tem conta de login (zumbi). Exclua-o pela tela Equipe."
      );
    }
    throw e;
  }
  return { ok: true };
});