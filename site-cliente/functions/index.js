const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
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

// ===== FASE 13: expansão de notificações (servidor, à prova de app fechado) =====

/** Pedido criado → notifica TODOS os donos. */
exports.notificarPedidoCriado = onDocumentCreated("pedidosPeca/{pedidoId}", async (event) => {
  const pedido = event.data && event.data.data();
  if (!pedido) return;

  const db = admin.firestore();
  const donos = await db.collection("usuarios").where("papel", "==", "dono").get();
  if (donos.empty) return;

  const batch = db.batch();
  donos.forEach((dono) => {
    batch.set(db.collection("notificacoes").doc(), {
      destinatarioUid: dono.id,
      titulo: "📦 Novo pedido de peça",
      mensagem: `${pedido.solicitanteNome} pediu ${pedido.quantidade}x ${pedido.marca} ${pedido.modelo}` +
        (pedido.osReferencia ? ` (OS: ${pedido.osReferencia})` : ""),
      tipo: "pedido_criado",
      lida: false,
      criadaEm: Date.now()
    });
  });
  await batch.commit();
});

/** Pedido respondeu/cancelou → notifica a outra ponta. */
exports.notificarPedidoMovimentado = onDocumentUpdated("pedidosPeca/{pedidoId}", async (event) => {
  const antes = event.data && event.data.before.data();
  const depois = event.data && event.data.after.data();
  if (!antes || !depois || antes.status === depois.status) return;
  if (antes.status !== "PENDENTE") return; // só a primeira decisão gera aviso

  const db = admin.firestore();

  // Funcionário cancelou → avisa os donos
  if (depois.status === "CANCELADO") {
    const donos = await db.collection("usuarios").where("papel", "==", "dono").get();
    const batch = db.batch();
    donos.forEach((dono) => {
      batch.set(db.collection("notificacoes").doc(), {
        destinatarioUid: dono.id,
        titulo: "📦 Pedido cancelado",
        mensagem: `${depois.solicitanteNome} cancelou o pedido de ${depois.quantidade}x ${depois.marca} ${depois.modelo}`,
        tipo: "pedido_cancelado",
        lida: false,
        criadaEm: Date.now()
      });
    });
    await batch.commit();
    return;
  }

  // Dono aprovou/recusou → avisa o solicitante
  const aprovado = depois.status === "APROVADO";
  await db.collection("notificacoes").add({
    destinatarioUid: depois.solicitanteUid,
    titulo: aprovado ? "✅ Pedido aprovado" : "❌ Pedido recusado",
    mensagem: aprovado
      ? `Seu pedido de ${depois.quantidade}x ${depois.marca} ${depois.modelo} foi aprovado.` +
        (depois.previsao ? ` Previsão: ${depois.previsao}.` : "") +
        (depois.mensagemResposta ? ` ${depois.mensagemResposta}` : "")
      : `Seu pedido de ${depois.quantidade}x ${depois.marca} ${depois.modelo} foi recusado. Motivo: ${depois.mensagemResposta}` +
        (depois.previsao ? ` Possível previsão: ${depois.previsao}.` : ""),
    tipo: "pedido_respondido",
    lida: false,
    criadaEm: Date.now()
  });
});