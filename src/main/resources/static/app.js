/* ====================================================================
   Carteira de Ação — Frontend
   Consome a API REST na mesma origem (Spring Boot static).
   ==================================================================== */

const API = ""; // mesma origem
const $ = (s, ctx = document) => ctx.querySelector(s);
const $$ = (s, ctx = document) => [...ctx.querySelectorAll(s)];

const state = {
  acoes: [],
  corretoras: [],
  operacoes: [],
};

/* ----------------------------- HTTP ------------------------------ */
async function api(path, options = {}) {
  const res = await fetch(API + path, {
    headers: { "Content-Type": "application/json", Accept: "application/json" },
    ...options,
  });

  let data = null;
  const text = await res.text();
  if (text) {
    try { data = JSON.parse(text); } catch { data = text; }
  }

  if (!res.ok) {
    const msg = extrairMensagemErro(data, res.status);
    const err = new Error(msg);
    err.payload = data;
    err.status = res.status;
    throw err;
  }
  return data;
}

function extrairMensagemErro(data, status) {
  if (data && typeof data === "object") {
    if (Array.isArray(data.detalhes) && data.detalhes.length) {
      return data.detalhes.join(" • ");
    }
    if (data.mensagem) return data.mensagem;
    if (data.message) return data.message;
    if (data.erro) return data.erro;
  }
  if (typeof data === "string" && data.trim()) return data;
  return `Erro ${status} ao comunicar com a API.`;
}

/* --------------------------- Utils ------------------------------- */
const onlyDigits = (s) => (s || "").replace(/\D/g, "");

function fmtMoeda(valor, moeda) {
  if (valor == null) return "—";
  const cur = moeda === "USD" ? "USD" : moeda === "BRL" ? "BRL" : moeda || "BRL";
  try {
    return new Intl.NumberFormat(cur === "USD" ? "en-US" : "pt-BR", {
      style: "currency", currency: cur,
    }).format(Number(valor));
  } catch {
    return `${valor} ${moeda || ""}`.trim();
  }
}

function fmtData(iso) {
  if (!iso) return "—";
  const d = new Date(iso);
  if (isNaN(d)) return iso;
  return d.toLocaleString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function fmtCnpj(c) {
  const d = onlyDigits(c);
  if (d.length !== 14) return c || "—";
  return `${d.slice(0,2)}.${d.slice(2,5)}.${d.slice(5,8)}/${d.slice(8,12)}-${d.slice(12)}`;
}
function fmtCep(c) {
  const d = onlyDigits(c);
  if (d.length !== 8) return c || "—";
  return `${d.slice(0,5)}-${d.slice(5)}`;
}
const esc = (s) => String(s ?? "").replace(/[&<>"']/g, (m) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[m]));

function mercadoTag(m) {
  return m === "EUA"
    ? `<span class="tag tag-eua">🇺🇸 EUA</span>`
    : `<span class="tag tag-br">🇧🇷 Brasil</span>`;
}

/* Formata resultado (lucro/prejuízo) com sinal, cor e percentual opcional */
function fmtResultado(valor, moeda, percentual) {
  if (valor == null) return '<span style="color:var(--txt-mute)">—</span>';
  const n = Number(valor);
  if (n === 0) return `<span class="pl pl-zero">${esc(fmtMoeda(0, moeda))}</span>`;
  const cls = n > 0 ? "pl-pos" : "pl-neg";
  const sinal = n > 0 ? "+" : "";
  const pct = percentual != null ? ` <small>(${sinal}${Number(percentual).toFixed(2)}%)</small>` : "";
  const ico = n > 0 ? "▲" : "▼";
  return `<span class="pl ${cls}">${ico} ${sinal}${esc(fmtMoeda(Math.abs(n), moeda).replace(/^-/, ""))}${pct}</span>`;
}

/* --------------------------- Toasts ------------------------------ */
function toast(titulo, msg = "", tipo = "ok") {
  const ico = tipo === "ok" ? "✅" : tipo === "err" ? "⛔" : "ℹ️";
  const el = document.createElement("div");
  el.className = `toast ${tipo}`;
  el.innerHTML = `<span class="t-ico">${ico}</span><div class="t-body"><b>${esc(titulo)}</b>${msg ? esc(msg) : ""}</div>`;
  $("#toasts").appendChild(el);
  setTimeout(() => {
    el.style.transition = "opacity .3s, transform .3s";
    el.style.opacity = "0";
    el.style.transform = "translateX(20px)";
    setTimeout(() => el.remove(), 320);
  }, tipo === "err" ? 6000 : 3800);
}

/* ----------------------- Navegação de views ---------------------- */
const VIEW_META = {
  dashboard: ["Visão geral", "Resumo da sua carteira e resultado dos investimentos"],
  acoes: ["Ações", "Cadastre, compre e venda — acompanhe lucro e prejuízo"],
  operacoes: ["Operações", "Histórico de compras e vendas com resultado realizado"],
  corretoras: ["Corretoras", "Instituições validadas via CNPJ, CEP e CVM"],
};

function setView(view) {
  $$(".nav-item").forEach((b) => b.classList.toggle("active", b.dataset.view === view));
  $$(".view").forEach((v) => v.classList.add("hidden"));
  $(`#view-${view}`).classList.remove("hidden");
  const [t, s] = VIEW_META[view] || VIEW_META.dashboard;
  $("#viewTitle").textContent = t;
  $("#viewSubtitle").textContent = s;
  $("#sidebar").classList.remove("open");
}

/* ---------------------------- Render ----------------------------- */
function moedaCarteira() {
  // usa a moeda predominante da carteira para os totais do dashboard
  const comPosicao = state.acoes.filter((a) => (a.quantidade || 0) > 0);
  const base = comPosicao.length ? comPosicao : state.acoes;
  const brl = base.filter((a) => a.moeda !== "USD").length;
  const usd = base.filter((a) => a.moeda === "USD").length;
  return usd > brl ? "USD" : "BRL";
}

function renderStats() {
  $("#statAcoes").textContent = state.acoes.length;
  $("#statCorretoras").textContent = state.corretoras.length;

  const moeda = moedaCarteira();
  const totInvestido = state.acoes.reduce((s, a) => s + Number(a.valorInvestido || 0), 0);
  const totAtual = state.acoes.reduce((s, a) => s + Number(a.valorAtual || 0), 0);
  const totNaoReal = state.acoes.reduce((s, a) => s + Number(a.lucroPrejuizoNaoRealizado || 0), 0);
  const totReal = state.acoes.reduce((s, a) => s + Number(a.lucroPrejuizoRealizado || 0), 0);

  $("#statInvestido").textContent = fmtMoeda(totInvestido, moeda);
  $("#statValorAtual").textContent = fmtMoeda(totAtual, moeda);

  const pctNaoReal = totInvestido > 0 ? (totNaoReal / totInvestido) * 100 : null;
  $("#statNaoRealizado").innerHTML = fmtResultado(totNaoReal, moeda, pctNaoReal);
  $("#statRealizado").innerHTML = fmtResultado(totReal, moeda, null);

  const dash = $("#dashAcoes");
  const recentes = [...state.acoes].slice(-6).reverse();
  if (!recentes.length) {
    dash.innerHTML = `<div class="empty">Nenhuma ação cadastrada ainda. Vá em <b>Ações</b> para começar.</div>`;
    return;
  }
  dash.innerHTML = recentes.map((a) => `
    <div class="mini-row">
      <span class="m-ticker">${esc(a.ticker)}</span>
      <span class="m-name">${esc(a.nomeEmpresa || "—")}</span>
      ${mercadoTag(a.mercado)}
      <span class="m-qtd">${(a.quantidade || 0) > 0 ? esc(a.quantidade) + " un." : "<span style='color:var(--txt-mute)'>sem posição</span>"}</span>
      <span class="m-price">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}</span>
    </div>`).join("");
}

function nomeCorretora(id) {
  if (id == null) return null;
  const c = state.corretoras.find((c) => c.id === id);
  return c ? (c.nomeFantasia || c.razaoSocial) : `#${id}`;
}

function renderAcoes() {
  const filtro = $("#filterAcoes").value.trim().toLowerCase();
  const tbody = $("#tableAcoes tbody");
  const lista = state.acoes.filter((a) =>
    !filtro ||
    (a.ticker || "").toLowerCase().includes(filtro) ||
    (a.nomeEmpresa || "").toLowerCase().includes(filtro)
  );

  $("#countAcoes").textContent = state.acoes.length;
  $("#emptyAcoes").classList.toggle("hidden", state.acoes.length > 0);

  tbody.innerHTML = lista.map((a) => {
    const temPosicao = (a.quantidade || 0) > 0;
    const resultado = temPosicao
      ? fmtResultado(a.lucroPrejuizoNaoRealizado, a.moeda, a.lucroPrejuizoNaoRealizadoPercentual)
      : '<span style="color:var(--txt-mute)">—</span>';
    return `
    <tr>
      <td class="ticker-cell">${esc(a.ticker)}</td>
      <td>${esc(a.nomeEmpresa || "—")}</td>
      <td>${mercadoTag(a.mercado)}</td>
      <td class="num price-cell">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}</td>
      <td class="num">${temPosicao ? esc(a.quantidade) : '<span style="color:var(--txt-mute)">0</span>'}</td>
      <td class="num price-cell">${temPosicao ? esc(fmtMoeda(a.precoMedio, a.moeda)) : '<span style="color:var(--txt-mute)">—</span>'}</td>
      <td class="num">${resultado}</td>
      <td class="acoes-col">
        <button class="btn btn-buy btn-icon" title="Comprar" data-buy="${a.id}">＋ Comprar</button>
        <button class="btn btn-sell btn-icon" title="Vender" data-sell="${a.id}" ${temPosicao ? "" : "disabled"}>－ Vender</button>
        <button class="btn btn-icon" title="Atualizar cotação" data-refresh-acao="${a.id}">⟳</button>
        <button class="btn btn-icon" title="Detalhes" data-detail-acao="${a.id}">⤢</button>
      </td>
    </tr>`;
  }).join("");

  if (state.acoes.length && !lista.length) {
    tbody.innerHTML = `<tr><td colspan="8" class="empty">Nenhum resultado para o filtro.</td></tr>`;
  }
}

function tipoTag(tipo) {
  return tipo === "COMPRA"
    ? '<span class="tag tag-buy">COMPRA</span>'
    : '<span class="tag tag-sell">VENDA</span>';
}

function renderOperacoes() {
  const filtro = $("#filterOperacoes").value.trim().toLowerCase();
  const tbody = $("#tableOperacoes tbody");
  const lista = state.operacoes.filter((o) =>
    !filtro || (o.ticker || "").toLowerCase().includes(filtro)
  );

  $("#countOperacoes").textContent = state.operacoes.length;
  $("#emptyOperacoes").classList.toggle("hidden", state.operacoes.length > 0);

  tbody.innerHTML = lista.map((o) => `
    <tr>
      <td>${esc(fmtData(o.dataHora))}</td>
      <td class="ticker-cell">${esc(o.ticker)}</td>
      <td>${tipoTag(o.tipo)}</td>
      <td class="num">${esc(o.quantidade)}</td>
      <td class="num price-cell">${esc(fmtMoeda(o.precoUnitario, o.moeda))}</td>
      <td class="num price-cell">${esc(fmtMoeda(o.valorTotal, o.moeda))}</td>
      <td class="num">${o.tipo === "VENDA" ? fmtResultado(o.resultado, o.moeda, o.resultadoPercentual) : '<span style="color:var(--txt-mute)">—</span>'}</td>
    </tr>`).join("");

  if (state.operacoes.length && !lista.length) {
    tbody.innerHTML = `<tr><td colspan="7" class="empty">Nenhum resultado para o filtro.</td></tr>`;
  }
}

function renderCorretoras() {
  const filtro = $("#filterCorretoras").value.trim().toLowerCase();
  const wrap = $("#cardsCorretoras");
  const lista = state.corretoras.filter((c) =>
    !filtro ||
    (c.razaoSocial || "").toLowerCase().includes(filtro) ||
    (c.nomeFantasia || "").toLowerCase().includes(filtro) ||
    onlyDigits(c.cnpj).includes(onlyDigits(filtro))
  );

  $("#countCorretoras").textContent = state.corretoras.length;
  $("#emptyCorretoras").classList.toggle("hidden", state.corretoras.length > 0);

  wrap.innerHTML = lista.map((c) => `
    <div class="ccard" data-detail-corretora="${c.id}">
      <div class="ccard-top">
        <div>
          <h3>${esc(c.nomeFantasia || c.razaoSocial || "—")}</h3>
          ${c.nomeFantasia && c.razaoSocial ? `<div class="fantasia">${esc(c.razaoSocial)}</div>` : ""}
        </div>
        ${c.validadaNaCvm ? '<span class="tag tag-cvm">✔ CVM</span>' : ""}
      </div>
      <div class="row"><span class="cnpj-mono">${esc(fmtCnpj(c.cnpj))}</span></div>
      <div class="row">📍 <b>${esc(c.cidade || "—")}${c.uf ? "/" + esc(c.uf) : ""}</b></div>
      ${c.situacaoCadastral ? `<div class="row">Situação: <b>${esc(c.situacaoCadastral)}</b></div>` : ""}
    </div>`).join("");

  if (state.corretoras.length && !lista.length) {
    wrap.innerHTML = `<div class="empty">Nenhum resultado para o filtro.</div>`;
  }
}

function renderSelectCorretoras() {
  const sel = $("#selectCorretora");
  const atual = sel.value;
  sel.innerHTML =
    `<option value="">Sem corretora vinculada</option>` +
    state.corretoras.map((c) =>
      `<option value="${c.id}">${esc(c.nomeFantasia || c.razaoSocial)} — ${esc(fmtCnpj(c.cnpj))}</option>`
    ).join("");
  sel.value = atual;
}

/* --------------------------- Carregar ---------------------------- */
async function carregarTudo() {
  try {
    const [acoes, corretoras, operacoes] = await Promise.all([
      api("/acoes"),
      api("/corretoras"),
      api("/operacoes"),
    ]);
    state.acoes = acoes || [];
    state.corretoras = corretoras || [];
    state.operacoes = operacoes || [];
    renderStats();
    renderAcoes();
    renderOperacoes();
    renderCorretoras();
    renderSelectCorretoras();
    setApiStatus(true);
  } catch (e) {
    setApiStatus(false);
    toast("Falha ao carregar dados", e.message, "err");
  }
}

function setApiStatus(online) {
  const el = $("#apiStatus");
  el.classList.toggle("online", online);
  el.classList.toggle("offline", !online);
  el.innerHTML = `<i class="dot"></i> ${online ? "API online" : "API offline"}`;
}

/* --------------------------- Detalhes ---------------------------- */
function abrirModal(html) {
  $("#modalContent").innerHTML = html;
  $("#modal").classList.remove("hidden");
}
function fecharModal() { $("#modal").classList.add("hidden"); }

function detalheAcaoHTML(a) {
  const temPosicao = (a.quantidade || 0) > 0;
  return `
    <h2>${esc(a.ticker)} ${mercadoTag(a.mercado)}</h2>
    <p class="sub">${esc(a.nomeEmpresa || "Empresa não informada")}</p>
    <div class="detail-grid">
      <div class="detail-item"><span class="k">Cotação atual</span><span class="v price-cell">${esc(fmtMoeda(a.cotacaoAtual, a.moeda))}</span></div>
      <div class="detail-item"><span class="k">Quantidade em carteira</span><span class="v">${temPosicao ? esc(a.quantidade) + " un." : "—"}</span></div>
      <div class="detail-item"><span class="k">Preço médio</span><span class="v price-cell">${temPosicao ? esc(fmtMoeda(a.precoMedio, a.moeda)) : "—"}</span></div>
      <div class="detail-item"><span class="k">Valor investido</span><span class="v">${temPosicao ? esc(fmtMoeda(a.valorInvestido, a.moeda)) : "—"}</span></div>
      <div class="detail-item"><span class="k">Valor atual</span><span class="v">${temPosicao ? esc(fmtMoeda(a.valorAtual, a.moeda)) : "—"}</span></div>
      <div class="detail-item"><span class="k">L/P não realizado</span><span class="v">${temPosicao ? fmtResultado(a.lucroPrejuizoNaoRealizado, a.moeda, a.lucroPrejuizoNaoRealizadoPercentual) : "—"}</span></div>
      <div class="detail-item"><span class="k">L/P realizado</span><span class="v">${fmtResultado(a.lucroPrejuizoRealizado, a.moeda, null)}</span></div>
      <div class="detail-item"><span class="k">Corretora</span><span class="v">${a.corretoraId != null ? esc(nomeCorretora(a.corretoraId)) : "—"}</span></div>
    </div>
    <div style="margin-top:22px; display:flex; gap:10px; flex-wrap:wrap">
      <button class="btn btn-buy" data-buy="${a.id}">＋ Comprar</button>
      <button class="btn btn-sell" data-sell="${a.id}" ${temPosicao ? "" : "disabled"}>－ Vender</button>
      <button class="btn" data-refresh-acao="${a.id}">⟳ Atualizar cotação</button>
    </div>`;
}

function detalheCorretoraHTML(c) {
  const endereco = [c.logradouro, c.numero, c.complemento].filter(Boolean).join(", ");
  return `
    <h2>${esc(c.nomeFantasia || c.razaoSocial)} ${c.validadaNaCvm ? '<span class="tag tag-cvm">✔ CVM</span>' : ""}</h2>
    <p class="sub">${esc(c.razaoSocial || "")}</p>
    <div class="detail-grid">
      <div class="detail-item"><span class="k">CNPJ</span><span class="v cnpj-mono">${esc(fmtCnpj(c.cnpj))}</span></div>
      <div class="detail-item"><span class="k">Situação cadastral</span><span class="v">${esc(c.situacaoCadastral || "—")}</span></div>
      <div class="detail-item"><span class="k">E-mail</span><span class="v">${esc(c.email || "—")}</span></div>
      <div class="detail-item"><span class="k">Telefone</span><span class="v">${esc(c.telefone || "—")}</span></div>
      <div class="detail-item detail-full"><span class="k">Endereço</span><span class="v">${esc(endereco || "—")}</span></div>
      <div class="detail-item"><span class="k">Bairro</span><span class="v">${esc(c.bairro || "—")}</span></div>
      <div class="detail-item"><span class="k">Cidade / UF</span><span class="v">${esc(c.cidade || "—")}${c.uf ? " / " + esc(c.uf) : ""}</span></div>
      <div class="detail-item"><span class="k">CEP</span><span class="v">${esc(fmtCep(c.cep))}</span></div>
      <div class="detail-item"><span class="k">Cadastrada em</span><span class="v">${esc(fmtData(c.dataCadastro))}</span></div>
    </div>`;
}

/* ----------------------- Modal de operação ----------------------- */
function abrirOperacaoModal(acaoId, tipo) {
  const a = state.acoes.find((x) => x.id === acaoId);
  if (!a) return;
  const isCompra = tipo === "COMPRA";
  const temPosicao = (a.quantidade || 0) > 0;
  const cotacao = a.cotacaoAtual != null ? Number(a.cotacaoAtual) : "";

  const posicaoInfo = temPosicao
    ? `<div class="op-info">Posição atual: <b>${esc(a.quantidade)} un.</b> a preço médio de <b>${esc(fmtMoeda(a.precoMedio, a.moeda))}</b></div>`
    : `<div class="op-info">Sem posição atual nesta ação.</div>`;

  abrirModal(`
    <h2>${isCompra ? "Comprar" : "Vender"} ${esc(a.ticker)} ${tipoTag(tipo)}</h2>
    <p class="sub">${esc(a.nomeEmpresa || "")}</p>
    ${posicaoInfo}
    <form id="formOperacao" class="form" data-acao="${a.id}" data-tipo="${tipo}" data-moeda="${esc(a.moeda)}">
      <label class="field">
        <span>Quantidade${isCompra ? "" : ` (máx. ${a.quantidade})`}</span>
        <input name="quantidade" type="number" min="1" ${isCompra ? "" : `max="${a.quantidade}"`} step="1" placeholder="Ex.: 100" required />
      </label>
      <label class="field">
        <span>Preço unitário (opcional)</span>
        <input name="precoUnitario" type="number" min="0" step="0.01" value="${cotacao}" placeholder="Usa a cotação atual se vazio" />
      </label>
      <div class="op-resumo" id="opResumo"></div>
      <button class="btn ${isCompra ? "btn-buy" : "btn-sell"}" type="submit" id="btnOperacao">
        ${isCompra ? "Confirmar compra" : "Confirmar venda"}
      </button>
      ${isCompra
        ? '<p class="form-hint">A compra recalcula o preço médio da sua posição.</p>'
        : '<p class="form-hint">A venda calcula o lucro/prejuízo: (preço de venda − preço médio) × quantidade.</p>'}
    </form>
  `);

  const form = $("#formOperacao");
  const calcResumo = () => {
    const qtd = Number(form.quantidade.value || 0);
    const preco = form.precoUnitario.value !== "" ? Number(form.precoUnitario.value) : Number(a.cotacaoAtual || 0);
    const box = $("#opResumo");
    if (!qtd || qtd <= 0 || !preco) { box.innerHTML = ""; return; }
    const total = qtd * preco;
    let html = `<div class="op-resumo-row"><span>Valor total</span><b>${esc(fmtMoeda(total, a.moeda))}</b></div>`;
    if (!isCompra && temPosicao) {
      const resultado = (preco - Number(a.precoMedio)) * qtd;
      html += `<div class="op-resumo-row"><span>Resultado estimado</span><b>${fmtResultado(resultado, a.moeda, null)}</b></div>`;
    }
    box.innerHTML = html;
  };
  form.quantidade.addEventListener("input", calcResumo);
  form.precoUnitario.addEventListener("input", calcResumo);
  calcResumo();
  form.addEventListener("submit", submitOperacao);
  setTimeout(() => form.quantidade.focus(), 50);
}

async function submitOperacao(e) {
  e.preventDefault();
  const form = e.target;
  const acaoId = Number(form.dataset.acao);
  const tipo = form.dataset.tipo;
  const btn = $("#btnOperacao");

  const quantidade = Number(form.quantidade.value);
  if (!quantidade || quantidade <= 0) return toast("Validação", "Informe uma quantidade válida.", "err");

  const body = { quantidade };
  if (form.precoUnitario.value !== "") body.precoUnitario = Number(form.precoUnitario.value);

  const endpoint = tipo === "COMPRA" ? "comprar" : "vender";
  setLoading(btn, true, tipo === "COMPRA" ? "Comprando…" : "Vendendo…");
  try {
    const op = await api(`/acoes/${acaoId}/${endpoint}`, { method: "POST", body: JSON.stringify(body) });
    if (tipo === "VENDA") {
      const r = Number(op.resultado);
      const txt = r >= 0 ? `Lucro de ${fmtMoeda(r, op.moeda)}` : `Prejuízo de ${fmtMoeda(Math.abs(r), op.moeda)}`;
      toast(`Venda de ${op.ticker}`, txt, r >= 0 ? "ok" : "err");
    } else {
      toast(`Compra de ${op.ticker}`, `${op.quantidade} un. por ${fmtMoeda(op.valorTotal, op.moeda)}`, "ok");
    }
    fecharModal();
    await carregarTudo();
  } catch (err) {
    toast(tipo === "COMPRA" ? "Falha na compra" : "Falha na venda", err.message, "err");
  } finally {
    setLoading(btn, false, tipo === "COMPRA" ? "Confirmar compra" : "Confirmar venda");
  }
}

/* ------------------------- Ações: submit ------------------------- */
async function submitAcao(e) {
  e.preventDefault();
  const btn = $("#btnSalvarAcao");
  const fd = new FormData(e.target);
  const body = {
    ticker: (fd.get("ticker") || "").trim().toUpperCase(),
    mercado: fd.get("mercado"),
  };
  const cid = fd.get("corretoraId");
  if (cid) body.corretoraId = Number(cid);

  if (!body.ticker) return toast("Validação", "Informe o ticker.", "err");

  setLoading(btn, true, "Cadastrando…");
  try {
    const nova = await api("/acoes", { method: "POST", body: JSON.stringify(body) });
    toast("Ação cadastrada", `${nova.ticker} — ${fmtMoeda(nova.cotacaoAtual, nova.moeda)}`, "ok");
    e.target.reset();
    await carregarTudo();
  } catch (err) {
    toast("Não foi possível cadastrar", err.message, "err");
  } finally {
    setLoading(btn, false, "Cadastrar ação");
  }
}

async function buscarAcao(e) {
  e.preventDefault();
  const ticker = $("#buscaTicker").value.trim();
  const box = $("#buscaAcaoResultado");
  if (!ticker) return;
  box.innerHTML = `<div class="loading-row"><span class="spin"></span> Buscando…</div>`;
  try {
    const a = await api(`/acoes/ticker/${encodeURIComponent(ticker)}`);
    box.innerHTML = `<div class="panel" style="background:rgba(8,12,26,.4)">${detalheAcaoHTML(a)}</div>`;
  } catch (err) {
    box.innerHTML = `<div class="empty">${esc(err.message)}</div>`;
  }
}

async function atualizarCotacao(id) {
  toast("Atualizando cotação…", "", "info");
  try {
    const a = await api(`/acoes/${id}/atualizar-cotacao`, { method: "PUT" });
    toast("Cotação atualizada", `${a.ticker} — ${fmtMoeda(a.cotacaoAtual, a.moeda)}`, "ok");
    await carregarTudo();
    if (!$("#modal").classList.contains("hidden")) {
      const atualizada = state.acoes.find((x) => x.id === id) || a;
      abrirModal(detalheAcaoHTML(atualizada));
    }
  } catch (err) {
    toast("Falha ao atualizar", err.message, "err");
  }
}

/* ----------------------- Corretoras: submit ---------------------- */
async function submitCorretora(e) {
  e.preventDefault();
  const btn = $("#btnSalvarCorretora");
  const fd = new FormData(e.target);
  const body = {
    cnpj: onlyDigits(fd.get("cnpj")),
    cep: onlyDigits(fd.get("cep")),
    numero: (fd.get("numero") || "").trim(),
    complemento: (fd.get("complemento") || "").trim(),
  };

  if (body.cnpj.length !== 14) return toast("Validação", "CNPJ deve conter 14 dígitos.", "err");
  if (body.cep.length !== 8) return toast("Validação", "CEP deve conter 8 dígitos.", "err");

  setLoading(btn, true, "Validando e cadastrando…");
  try {
    const nova = await api("/corretoras", { method: "POST", body: JSON.stringify(body) });
    toast("Corretora cadastrada", nova.nomeFantasia || nova.razaoSocial, "ok");
    e.target.reset();
    await carregarTudo();
  } catch (err) {
    toast("Não foi possível cadastrar", err.message, "err");
  } finally {
    setLoading(btn, false, "Cadastrar corretora");
  }
}

async function buscarCorretora(e) {
  e.preventDefault();
  const cnpj = onlyDigits($("#buscaCnpj").value);
  const box = $("#buscaCorretoraResultado");
  if (!cnpj) return;
  box.innerHTML = `<div class="loading-row"><span class="spin"></span> Buscando…</div>`;
  try {
    const c = await api(`/corretoras/cnpj/${encodeURIComponent(cnpj)}`);
    box.innerHTML = `<div class="panel" style="background:rgba(8,12,26,.4)">${detalheCorretoraHTML(c)}</div>`;
  } catch (err) {
    box.innerHTML = `<div class="empty">${esc(err.message)}</div>`;
  }
}

/* ----------------------------- UI helpers ------------------------ */
function setLoading(btn, loading, label) {
  if (!btn) return;
  btn.disabled = loading;
  btn.innerHTML = loading ? `<span class="spin"></span> ${label}` : label;
}

/* ---------------------------- Eventos ---------------------------- */
function bind() {
  $$(".nav-item").forEach((b) => b.addEventListener("click", () => setView(b.dataset.view)));
  $$("[data-goto]").forEach((b) => b.addEventListener("click", () => setView(b.dataset.goto)));
  $("#menuToggle").addEventListener("click", () => $("#sidebar").classList.toggle("open"));
  $("#refreshAll").addEventListener("click", carregarTudo);

  $("#formAcao").addEventListener("submit", submitAcao);
  $("#formBuscaAcao").addEventListener("submit", buscarAcao);
  $("#formCorretora").addEventListener("submit", submitCorretora);
  $("#formBuscaCorretora").addEventListener("submit", buscarCorretora);

  $("#filterAcoes").addEventListener("input", renderAcoes);
  $("#filterOperacoes").addEventListener("input", renderOperacoes);
  $("#filterCorretoras").addEventListener("input", renderCorretoras);

  $("#modalClose").addEventListener("click", fecharModal);
  $("#modal").addEventListener("click", (e) => { if (e.target.id === "modal") fecharModal(); });
  document.addEventListener("keydown", (e) => { if (e.key === "Escape") fecharModal(); });

  // Delegação para botões dinâmicos
  document.addEventListener("click", (e) => {
    const buy = e.target.closest("[data-buy]");
    if (buy) { abrirOperacaoModal(Number(buy.dataset.buy), "COMPRA"); return; }

    const sell = e.target.closest("[data-sell]");
    if (sell && !sell.disabled) { abrirOperacaoModal(Number(sell.dataset.sell), "VENDA"); return; }

    const r = e.target.closest("[data-refresh-acao]");
    if (r) { atualizarCotacao(Number(r.dataset.refreshAcao)); return; }

    const da = e.target.closest("[data-detail-acao]");
    if (da) {
      const a = state.acoes.find((x) => x.id === Number(da.dataset.detailAcao));
      if (a) abrirModal(detalheAcaoHTML(a));
      return;
    }
    const dc = e.target.closest("[data-detail-corretora]");
    if (dc) {
      const c = state.corretoras.find((x) => x.id === Number(dc.dataset.detailCorretora));
      if (c) abrirModal(detalheCorretoraHTML(c));
      return;
    }
  });
}

/* ----------------------------- Init ------------------------------ */
bind();
carregarTudo();
