// ChicoCerveja — Integração Spring Boot + Criação Planejada + Baixa no Início da Produção

const API_URL = "/api";

async function requisicaoAPI(endpoint, options = {}) {
  let resposta;
  try {
    resposta = await fetch(`${API_URL}${endpoint}`, {
      ...options,
      headers: {
        Accept: "application/json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers
      }
    });
  } catch (erro) {
    throw new Error("Erro de conexão com o servidor.");
  }

  if (!resposta.ok) {
    const detalhe = await resposta.text();
    throw new Error(`Erro ${resposta.status}: ` + (detalhe || resposta.statusText));
  }

  if (resposta.status === 204) return null;
  const texto = await resposta.text();
  return texto ? JSON.parse(texto) : null;
}

const api = {
  // --- ESTOQUE ---
  listarEstoque() { return requisicaoAPI("/estoque"); },
  buscarEstoque(id) { return requisicaoAPI(`/estoque/${id}`); },
  cadastrarEstoque(dados) {
    return requisicaoAPI("/estoque", { method: "POST", body: JSON.stringify(dados) });
  },
  deletarEstoque(id) {
    return requisicaoAPI(`/estoque/${id}`, { method: "DELETE" });
  },
  adicionarQuantidade(id, quantidade, origem = "AJUSTE_MANUAL") {
    return requisicaoAPI(`/estoque/${id}/entrada`, {
      method: "POST",
      body: JSON.stringify({ quantidade, origem })
    });
  },
  retirarQuantidade(id, quantidade, origem = "AJUSTE_MANUAL") {
    return requisicaoAPI(`/estoque/${id}/saida`, {
      method: "POST",
      body: JSON.stringify({ quantidade, origem })
    });
  },

  // --- MOVIMENTAÇÕES ---
  listarMovimentacoes() { return requisicaoAPI("/movimentacoes"); },

  // --- PRODUÇÃO ---
  listarProducoes() { return requisicaoAPI("/producao"); },
  buscarProducao(id) { return requisicaoAPI(`/producao/${id}`); },
  cadastrarProducao(dados) {
    return requisicaoAPI("/producao", { method: "POST", body: JSON.stringify(dados) });
  },
  atualizarStatusProducao(id, status) {
    return requisicaoAPI(`/producao/${id}/status`, { method: "PUT", body: JSON.stringify({ status }) });
  },
  iniciarProducao(id) {
    return requisicaoAPI(`/producao/${id}/iniciar`, { method: "POST" });
  },
  deletarProducao(id) {
    return requisicaoAPI(`/producao/${id}`, { method: "DELETE" });
  },

  // --- INGREDIENTES DA PRODUÇÃO ---
  listarIngredientesProducao(producaoId) {
    return requisicaoAPI(`/producao/${producaoId}/ingredientes`);
  },
  adicionarIngredienteProducao(producaoId, dados) {
    return requisicaoAPI(`/producao/${producaoId}/ingredientes`, { method: "POST", body: JSON.stringify(dados) });
  },
  removerIngredienteProducao(producaoId, ingredienteId) {
    return requisicaoAPI(`/producao/${producaoId}/ingredientes/${ingredienteId}`, { method: "DELETE" });
  }
};

// --- DOM & UTILITÁRIOS ---
const $ = (selector, root = document) => root.querySelector(selector); const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

const dateLabel = v => v ? new Date(v).toLocaleDateString('pt-BR') : '—';
const dateISO = (d = new Date()) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const number = v => Number(v || 0).toLocaleString('pt-BR', { maximumFractionDigits: 2 });

const categoryLabels = { MALTE: 'Malte', LUPULO: 'Lúpulo', LEVEDURA: 'Levedura', OUTROS: 'Outros' };
const statusLabels = { PENDENTE: 'Pendente', MOSTURACAO: 'Mosturação', FERVURA: 'Fervura', FERMENTACAO: 'Fermentação', FINALIZADA: 'Finalizada' };
const originLabels = { CADASTRO: 'Cadastro', AJUSTE_MANUAL: 'Ajuste manual', PRODUCAO: 'Produção' };

let currentCategory = 'TODOS';
let currentProductionFilter = 'TODOS';
let activeProductionId = null;
let draftIngredients = [];
let stock = [];
let movements = [];
let productions = [];

// Variáveis de Paginação
let stockPage = 1;
const STOCK_PER_PAGE = 7;
let movementPage = 1;
const MOV_PER_PAGE = 5;

function toast(message, type = 'success') {
  const region = $('#toast-region');
  if (!region) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  el.textContent = message;
  region.appendChild(el);
  setTimeout(() => el.remove(), 3300);
}

function showPage(page) {
  $$('.page').forEach(el => el.classList.toggle('active', el.id === `page-${page}`));
  $$('.nav-link').forEach(el => el.classList.toggle('active', el.dataset.page === page || (page === 'detalhe' && el.dataset.page === 'producao')));

  const names = { estoque: 'Estoque', receitas: 'Receitas', producao: 'Produção', detalhe: 'Detalhes da produção' };
  if ($('#crumb-current')) $('#crumb-current').textContent = names[page] || page;

  window.scrollTo({ top: 0, behavior: 'smooth' });

  if (page === 'estoque') carregarEstoque();
  if (page === 'producao') carregarProducoes();
  if (page === 'receitas') carregarEstoque().then(() => { refreshIngredientSelects(); updateStockHint(); });
}

function categoryIcon(cat) {
  return cat === 'MALTE' ? 'M' : cat === 'LUPULO' ? 'L' : cat === 'LEVEDURA' ? 'Y' : '✦';
}

function getStock(id) {
  return stock.find(item => String(item.id) === String(id));
}

function escapeHtml(value) {
  return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

// --- CARREGAMENTO DE DADOS ---
async function carregarEstoque() {
  try {
    stock = (await api.listarEstoque()) || [];
  } catch (err) {
    stock = [];
  }
  renderStock();
  await carregarMovimentacoes();
}

async function carregarMovimentacoes() {
  try {
    movements = (await api.listarMovimentacoes()) || [];
    movements.sort((a, b) => new Date(b.data_movimentacao || b.data || 0) - new Date(a.data_movimentacao || a.data || 0));
  } catch (err) {
    movements = [];
  }
  renderMovements();
}

// --- RENDERIZAÇÃO & PAGINAÇÃO DE ESTOQUE ---
function renderStock() {
  const query = ($('#stock-search')?.value || '').trim().toLowerCase();
  const filtered = stock.filter(i => (currentCategory === 'TODOS' || i.categoria === currentCategory) &&
      `${i.nome} ${i.marca || ''} ${categoryLabels[i.categoria]}`.toLowerCase().includes(query));

  const tbody = $('#stock-table');
  if (!tbody) return;

  if (!filtered.length) {
    tbody.innerHTML = '<tr><td colspan="6" class="empty-inline">Nenhum ingrediente encontrado.</td></tr>';
    if ($('#stock-pagination')) $('#stock-pagination').innerHTML = '';
    if ($('#stock-result-count')) $('#stock-result-count').textContent = '0 itens';
    return;
  }

  const totalPages = Math.ceil(filtered.length / STOCK_PER_PAGE);
  if (stockPage > totalPages) stockPage = totalPages;
  if (stockPage < 1) stockPage = 1;

  const start = (stockPage - 1) * STOCK_PER_PAGE;
  const items = filtered.slice(start, start + STOCK_PER_PAGE);

  tbody.innerHTML = items.map(item => `
    <tr>
      <td><div class="ingredient-name"><span class="ingredient-avatar ${(item.categoria || 'OUTROS').toLowerCase().replace('outros','other')}">${categoryIcon(item.categoria)}</span><span>${escapeHtml(item.nome)}</span></div></td>
      <td><span class="category-label ${item.categoria}">${categoryLabels[item.categoria] || item.categoria}</span></td>
      <td>${escapeHtml(item.marca || '—')}</td>
      <td><span class="qty-strong">${number(item.quantidade)} kg</span></td>
      <td>${dateLabel(item.data_cadastro)}</td>
      <td>
        <div class="table-actions">
          <button class="mini-action" data-stock-quantity="ENTRADA" data-stock-id="${item.id}">＋</button>
          <button class="mini-action" data-stock-quantity="SAIDA" data-stock-id="${item.id}">−</button>
          <button class="mini-action remove" data-stock-delete="${item.id}">×</button>
        </div>
      </td>
    </tr>`).join('');

  if ($('#stock-result-count')) $('#stock-result-count').textContent = `${filtered.length} itens (Pág. ${stockPage} de ${totalPages})`;
  if ($('#stat-items')) $('#stat-items').textContent = stock.length;
  if ($('#stat-quantity')) $('#stat-quantity').innerHTML = `${number(stock.reduce((s, i) => s + (i.quantidade || 0), 0))} <em>kg</em>`;
  if ($('#count-all')) $('#count-all').textContent = stock.length;

  renderStockPagination(totalPages);
}

function renderStockPagination(totalPages) {
  const c = $('#stock-pagination');
  if (!c) return;
  if (totalPages <= 1) { c.innerHTML = ''; return; }

  let h = `<button class="page-btn" id="sp-prev" ${stockPage === 1 ? 'disabled' : ''}>← Anterior</button>`;
  for (let i = 1; i <= totalPages; i++) h += `<button class="page-btn ${i === stockPage ? 'active' : ''}" data-sp="${i}">${i}</button>`;
  h += `<button class="page-btn" id="sp-next" ${stockPage === totalPages ? 'disabled' : ''}>Próxima →</button>`;
  c.innerHTML = h;

  c.querySelectorAll('[data-sp]').forEach(b => {
    b.onclick = () => { stockPage = Number(b.getAttribute('data-sp')); renderStock(); };
  });

  const prev = $('#sp-prev');
  if (prev) prev.onclick = () => { if (stockPage > 1) { stockPage--; renderStock(); } };

  const next = $('#sp-next');
  if (next) next.onclick = () => { if (stockPage < totalPages) { stockPage++; renderStock(); } };
}

// --- RENDERIZAÇÃO & PAGINAÇÃO DE MOVIMENTAÇÕES ---
function renderMovements() {
  const filter = $('#movement-filter')?.value || 'TODAS';
  const filtered = movements.filter(m => filter === 'TODAS' || (m.tipoMovimentacao || m.tipo) === filter);

  const tbody = $('#movement-table');
  const empty = $('#movement-empty');
  if (!tbody) return;

  if (!filtered.length) {
    tbody.innerHTML = '';
    if (empty) empty.classList.remove('hidden');
    if ($('#movement-pagination')) $('#movement-pagination').innerHTML = '';
    return;
  }
  if (empty) empty.classList.add('hidden');

  const totalPages = Math.ceil(filtered.length / MOV_PER_PAGE);
  if (movementPage > totalPages) movementPage = totalPages;
  if (movementPage < 1) movementPage = 1;

  const start = (movementPage - 1) * MOV_PER_PAGE;
  const items = filtered.slice(start, start + MOV_PER_PAGE);

  tbody.innerHTML = items.map(m => {
    const tipo = m.tipoMovimentacao || m.tipo;
    const origem = m.orgiemMovimentacao || m.origemMovimentacao || m.origem;
    const qtd = m.quantidade_movimentacao || m.quantidade;
    const nome = m.estoque ? m.estoque.nome : (m.nome || 'Ingrediente');
    const data = m.data_movimentacao || m.data;

    return `<tr>
      <td>${dateLabel(data)}</td>
      <td><strong>${escapeHtml(nome)}</strong></td>
      <td><span class="movement-tag ${tipo}">${tipo === 'ENTRADA' ? 'Entrada' : 'Saída'}</span></td>
      <td>${originLabels[origem] || origem}</td>
      <td class="movement-amount ${tipo}">${tipo === 'ENTRADA' ? '+' : '−'}${number(qtd)} kg</td>
    </tr>`;
  }).join('');

  if ($('#stat-movements')) $('#stat-movements').textContent = movements.length;
  renderMovPagination(totalPages);
}

function renderMovPagination(totalPages) {
  const c = $('#movement-pagination');
  if (!c) return;
  if (totalPages <= 1) { c.innerHTML = ''; return; }

  let h = `<button class="page-btn" id="mp-prev" ${movementPage === 1 ? 'disabled' : ''}>← Anterior</button>`;
  for (let i = 1; i <= totalPages; i++) h += `<button class="page-btn ${i === movementPage ? 'active' : ''}" data-mp="${i}">${i}</button>`;
  h += `<button class="page-btn" id="mp-next" ${movementPage === totalPages ? 'disabled' : ''}>Próxima →</button>`;
  c.innerHTML = h;

  c.querySelectorAll('[data-mp]').forEach(b => {
    b.onclick = () => { movementPage = Number(b.getAttribute('data-mp')); renderMovements(); };
  });

  const prev = $('#mp-prev');
  if (prev) prev.onclick = () => { if (movementPage > 1) { movementPage--; renderMovements(); } };

  const next = $('#mp-next');
  if (next) next.onclick = () => { if (movementPage < totalPages) { movementPage++; renderMovements(); } };
}

// --- PRODUÇÃO ---
async function carregarProducoes() {
  try {
    productions = (await api.listarProducoes()) || [];
  } catch (err) {
    productions = [];
  }
  renderProductions();
}

// --- FILTRO E RENDERIZAÇÃO DE PRODUÇÕES CORRIGIDO ---

function renderProductions() {
  const query = ($('#production-search')?.value || '').trim().toLowerCase();

  const filtered = productions.filter(p => {
    const status = p.status_producao;

    // Categorização dos status
    let matchCategory = false;
    if (currentProductionFilter === 'TODOS') {
      matchCategory = true;
    } else if (currentProductionFilter === 'PENDENTE') {
      matchCategory = (status === 'PENDENTE');
    } else if (currentProductionFilter === 'FINALIZADA') {
      matchCategory = (status === 'FINALIZADA');
    } else if (currentProductionFilter === 'EM_ANDAMENTO') {
      // Agrupa todas as fases ativas de fabricação
      matchCategory = ['MOSTURACAO', 'FERVURA', 'FERMENTACAO'].includes(status);
    } else {
      matchCategory = (status === currentProductionFilter);
    }

    const matchQuery = (p.nome_cerveja || '').toLowerCase().includes(query);
    return matchCategory && matchQuery;
  });

  if ($('#prod-count-all')) $('#prod-count-all').textContent = productions.length;

  const grid = $('#production-grid');
  const empty = $('#production-empty');
  if (!grid) return;

  if (!filtered.length) {
    grid.innerHTML = '';
    if (empty) empty.classList.remove('hidden');
    return;
  }
  if (empty) empty.classList.add('hidden');

  grid.innerHTML = filtered.map(p => `
    <article class="production-card" tabindex="0" role="button" data-production-open="${p.id}">
      <div class="prod-card-top">
        <div class="beer-mark">♧</div>
        <span class="status-pill ${p.status_producao === 'PENDENTE' ? 'pending' : p.status_producao === 'FINALIZADA' ? 'done' : 'progress'}">
          ${statusLabels[p.status_producao] || p.status_producao}
        </span>
      </div>
      <h3>${escapeHtml(p.nome_cerveja)}</h3>
      <div class="prod-date">Data: ${dateLabel(p.data_producao)}</div>
      <div class="prod-card-divider"></div>
      <div class="prod-card-metrics">
        <div class="prod-metric"><span>Volume</span><strong>${number(p.quantidade_producao)} L</strong></div>
      </div>
      <div class="prod-card-bottom"><span class="open-detail">Ver detalhes →</span></div>
    </article>`).join('');
}

async function openProduction(id) {
  try {
    const p = await api.buscarProducao(id);
    const ingredientes = (await api.listarIngredientesProducao(id)) || [];
    activeProductionId = p.id;

    if ($('#detail-title')) $('#detail-title').textContent = p.nome_cerveja;
    if ($('#detail-subtitle')) $('#detail-subtitle').textContent = `Previsão: ${dateLabel(p.data_producao)}`;
    if ($('#detail-status')) {
      $('#detail-status').className = `status-pill ${p.status_producao === 'PENDENTE' ? 'pending' : p.status_producao === 'FINALIZADA' ? 'done' : 'progress'}`;
      $('#detail-status').textContent = statusLabels[p.status_producao] || p.status_producao;
    }
    if ($('#detail-phase')) $('#detail-phase').value = p.status_producao;

    if ($('#detail-stats')) {
      $('#detail-stats').innerHTML = `
        <div class="detail-stat"><span>Volume</span><strong>${number(p.quantidade_producao)} L</strong></div>
        <div class="detail-stat"><span>Itens</span><strong>${ingredientes.length}</strong></div>
        <div class="detail-stat"><span>Total Insumos</span><strong>${number(ingredientes.reduce((s, i) => s + (i.quantidade_utilizada || 0), 0))} kg</strong></div>`;
    }

    renderProductionIngredients(ingredientes);
    showPage('detalhe');
  } catch (err) {
    toast(err.message, 'error');
  }
}

function renderProductionIngredients(ingredientes) {
  const tbody = $('#detail-ingredients-table');
  const empty = $('#detail-ingredients-empty');
  if (!tbody) return;

  if (!ingredientes.length) {
    tbody.innerHTML = '';
    if (empty) empty.classList.remove('hidden');
    return;
  }
  if (empty) empty.classList.add('hidden');

  tbody.innerHTML = ingredientes.map(i => `
    <tr>
      <td><div class="ingredient-name"><span class="ingredient-avatar ${(i.estoque?.categoria || 'OUTROS').toLowerCase().replace('outros','other')}">${categoryIcon(i.estoque?.categoria)}</span><span>${escapeHtml(i.estoque?.nome || 'Ingrediente')}</span></div></td>
      <td><strong>${number(i.quantidade_utilizada)} kg</strong></td>
      <td>${number(i.temperatura)} °C</td>
      <td>${number(i.tempo)} min</td>
      <td class="right"><button class="mini-action remove" data-detail-remove="${i.id}">Remover</button></td>
    </tr>`).join('');
}

// --- MONTAGEM DE RECEITA ---
function refreshIngredientSelects() {
  const category = $('#ingredient-category')?.value || 'TODOS';
  const options = stock.filter(i => (i.quantidade || 0) > 0 && (category === 'TODOS' || i.categoria === category));

  const select = $('#ingredient-select');
  if (select) {
    if (options.length > 0) {
      select.innerHTML = options.map(i => `<option value="${i.id}">${escapeHtml(i.nome)} · ${number(i.quantidade)} kg disponíveis</option>`).join('');
      select.disabled = false;
    } else {
      select.innerHTML = '<option value="">Nenhum ingrediente disponível nesta categoria</option>';
      select.disabled = true;
    }
  }

  if ($('#detail-ingredient-select')) {
    const detailOpts = stock.filter(i => (i.quantidade || 0) > 0);
    $('#detail-ingredient-select').innerHTML = detailOpts.length
        ? detailOpts.map(i => `<option value="${i.id}">${escapeHtml(i.nome)} · ${number(i.quantidade)} kg</option>`).join('')
        : '<option value="">Nenhum item disponível</option>';
  }
}

function updateStockHint() {
  const select = $('#ingredient-select');
  const hint = $('#ingredient-stock-hint');
  if (!select || !hint) return;

  const id = select.value;
  const item = getStock(id);

  if (item) {
    hint.innerHTML = `Saldo em estoque de <strong>${escapeHtml(item.nome)}</strong>: <strong>${number(item.quantidade)} kg</strong>`;
  } else {
    hint.textContent = 'Selecione um ingrediente para consultar o saldo disponível.';
  }
}

function renderDraftIngredients() {
  if ($('#selected-count')) $('#selected-count').textContent = `${draftIngredients.length} ingredientes`;
  if ($('#recipe-total-items')) $('#recipe-total-items').textContent = draftIngredients.length;
  if ($('#recipe-total-amount')) $('#recipe-total-amount').textContent = `${number(draftIngredients.reduce((s, i) => s + i.quantidade, 0))} kg`;

  const list = $('#recipe-ingredient-list');
  if (!list) return;

  if (!draftIngredients.length) {
    list.innerHTML = '<div class="empty-inline">Sua lista está vazia. Adicione os ingredientes acima.</div>';
    return;
  }

  list.innerHTML = draftIngredients.map((i, index) => {
    const item = getStock(i.estoqueId);
    return `<div class="recipe-ingredient-row">
      <div><div class="recipe-item-title">${escapeHtml(item?.nome || 'Ingrediente')}</div><div class="recipe-item-meta">${categoryLabels[item?.categoria] || ''} · disponível: ${number(item?.quantidade || 0)} kg</div></div>
      <div class="recipe-value"><small>Quantidade</small>${number(i.quantidade)} kg</div>
      <div class="recipe-value"><small>Temperatura</small>${number(i.temperatura)} °C</div>
      <div class="recipe-value"><small>Tempo</small>${number(i.tempo)} min</div>
      <button class="remove-row" type="button" data-draft-remove="${index}">×</button>
    </div>`;
  }).join('');
}

// --- INICIALIZAÇÃO DA APLICAÇÃO ---
document.addEventListener('DOMContentLoaded', () => {
  $$('.nav-link').forEach(btn => btn.addEventListener('click', () => showPage(btn.dataset.page)));$$
  ('[data-go-page]').forEach(btn => btn.addEventListener('click', () => showPage(btn.dataset.goPage)));

  if ($('#production-grid')) {
    $('#production-grid').onclick = e => {
      const card = e.target.closest('[data-production-open]');
      if (card) openProduction(card.getAttribute('data-production-open'));
    };
  }

  if ($('#open-stock-modal')) {
    $('#open-stock-modal').onclick = () => {
      if ($('#stock-form')) $('#stock-form').reset();
      if ($('#stock-edit-id')) $('#stock-edit-id').value = '';
      if ($('#stock-date')) $('#stock-date').value = dateISO();
      if ($('#stock-modal-backdrop')) $('#stock-modal-backdrop').classList.remove('hidden'); }; }    $$('[data-close-modal]').forEach(b => b.onclick = () => $('#stock-modal-backdrop')?.classList.add('hidden')); $$('[data-close-quantity]').forEach(b => b.onclick = () => $('#quantity-modal-backdrop')?.classList.add('hidden')); $$('[data-close-detail-modal]').forEach(b => b.onclick = () =>$('#detail-ingredient-modal')?.classList.add('hidden'));

  // Cadastrar Item no Estoque
  if ($('#stock-form')) {
    $('#stock-form').onsubmit = async e => {
      e.preventDefault();
      try {
        await api.cadastrarEstoque({
          nome: $('#stock-name').value,
          categoria: $('#stock-category').value,
          marca: $('#stock-brand').value,
          quantidade: parseFloat($('#stock-quantity').value),
          data_cadastro: $('#stock-date').value
        });
        toast('Item salvo com sucesso!');
        $('#stock-modal-backdrop')?.classList.add('hidden');
        carregarEstoque();
      } catch (err) { toast(err.message, 'error'); }
    };
  }

  // Tabela de Estoque
  if ($('#stock-table')) {
    $('#stock-table').onclick = async e => {
      const del = e.target.closest('[data-stock-delete]');
      if (del && confirm('Deseja excluir este item do estoque?')) {
        try { await api.deletarEstoque(del.getAttribute('data-stock-delete')); toast('Item removido!'); carregarEstoque(); }
        catch (err) { toast(err.message, 'error'); }
        return;
      }
      const qty = e.target.closest('[data-stock-quantity]');
      if (qty) {
        if ($('#quantity-item-id')) $('#quantity-item-id').value = qty.getAttribute('data-stock-id');
        if ($('#quantity-kind')) $('#quantity-kind').value = qty.getAttribute('data-stock-quantity');
        if ($('#quantity-modal-backdrop')) $('#quantity-modal-backdrop').classList.remove('hidden');
      }
    };
  }

  // Entrada e Saída Manual
  if ($('#quantity-form')) {
    $('#quantity-form').onsubmit = async e => {
      e.preventDefault();
      const id = $('#quantity-item-id').value;
      const kind = $('#quantity-kind').value;
      const val = parseFloat($('#quantity-value').value);
      try {
        if (kind === 'ENTRADA') await api.adicionarQuantidade(id, val, "AJUSTE_MANUAL");
        else await api.retirarQuantidade(id, val, "AJUSTE_MANUAL");
        toast('Estoque atualizado!');
        $('#quantity-modal-backdrop')?.classList.add('hidden');
        carregarEstoque();
      } catch (err) { toast(err.message, 'error'); }
    };
  }

  // Seletores e Dicas da Tela de Receita
  if ($('#ingredient-category')) {
    $('#ingredient-category').onchange = () => {
      refreshIngredientSelects();
      updateStockHint();
    };
  }

  if ($('#ingredient-select')) {
    $('#ingredient-select').onchange = updateStockHint;
  }

  // Adicionar Ingrediente Rascunho
  if ($('#add-ingredient')) {
    $('#add-ingredient').onclick = () => {
      const select = $('#ingredient-select');
      const id = select?.value;
      const amountInput = $('#ingredient-amount');
      const amount = parseFloat(amountInput?.value);

      if (!id) return toast('Selecione um ingrediente válido.', 'error');
      const item = getStock(id);
      if (!item) return toast('Ingrediente não encontrado no estoque.', 'error');
      if (isNaN(amount) || amount <= 0) return toast('Informe uma quantidade válida.', 'error');
      if (amount > item.quantidade) return toast(`Quantidade informada (${amount} kg) é maior que o saldo em estoque (${item.quantidade} kg).`, 'error');

      draftIngredients.push({
        estoqueId: id,
        quantidade: amount,
        temperatura: parseFloat($('#ingredient-temp')?.value) || 0,
        tempo: parseInt($('#ingredient-time')?.value) || 0
      });

      renderDraftIngredients();

      if (amountInput) amountInput.value = '';
      if ($('#ingredient-temp')) $('#ingredient-temp').value = '';
      if ($('#ingredient-time')) $('#ingredient-time').value = '';

      toast('Ingrediente adicionado à receita!');
    };
  }

  if ($('#recipe-ingredient-list')) {
    $('#recipe-ingredient-list').onclick = e => {
      const btn = e.target.closest('[data-draft-remove]');
      if (btn) {
        draftIngredients.splice(Number(btn.getAttribute('data-draft-remove')), 1);
        renderDraftIngredients();
      }
    };
  }

  // Salvar Produção Planejada (sem dar baixa antecipada no estoque)
  if ($('#recipe-form')) {
    $('#recipe-form').onsubmit = async e => {
      e.preventDefault();

      if (draftIngredients.length === 0) {
        return toast('Adicione pelo menos um ingrediente para criar a receita.', 'error');
      }

      try {
        // 1. Cadastra a produção com status PENDENTE
        const prod = await api.cadastrarProducao({
          nome_cerveja: $('#recipe-name').value,
          data_producao: $('#recipe-date').value,
          quantidade_producao: parseFloat($('#recipe-quantity').value)
        });

        // 2. Apenas vincula os ingredientes à produção
        for (const ing of draftIngredients) {
          await api.adicionarIngredienteProducao(prod.id, ing);
        }

        draftIngredients = [];
        $('#recipe-form').reset();
        renderDraftIngredients();
        await carregarEstoque();

        toast('Produção cadastrada com sucesso!');
        showPage('producao');
      } catch (err) { toast(err.message || 'Erro ao criar produção.', 'error'); }
    };
  }

  if ($('#back-to-productions')) $('#back-to-productions').onclick = () => showPage('producao');

  // Atualizar Fase da Produção / Iniciar Processo
  if ($('#save-phase')) {
    $('#save-phase').onclick = async () => {
      const statusDesejado = $('#detail-phase').value;
      try {
        const producaoAtual = await api.buscarProducao(activeProductionId);

        // Se a produção está PENDENTE e o usuário selecionou MOSTURACAO (ou qualquer fase adiante):
        if (producaoAtual.status_producao === 'PENDENTE' && statusDesejado !== 'PENDENTE') {
          // 1. Executa o início do lote (dá baixa no estoque e muda para MOSTURACAO)
          await api.iniciarProducao(activeProductionId);

          // 2. Se o usuário escolheu uma fase além de MOSTURACAO (ex: FERVURA), atualiza o status em seguida
          if (statusDesejado !== 'MOSTURACAO') {
            await api.atualizarStatusProducao(activeProductionId, statusDesejado);
          }

          toast('Produção iniciada com sucesso! Baixa realizada no estoque.');
        } else {
          // Se já não está mais pendente, apenas atualiza o status normalmente
          await api.atualizarStatusProducao(activeProductionId, statusDesejado);
          toast('Fase atualizada!');
        }

        await carregarEstoque();
        openProduction(activeProductionId);
      } catch (err) {
        toast(err.message, 'error');
      }
    };
  }

  if ($('#delete-production')) {
    $('#delete-production').onclick = async () => {
      if (confirm('Deseja excluir esta produção?')) {
        try {
          await api.deletarProducao(activeProductionId);
          toast('Produção excluída!');
          await carregarEstoque();
          showPage('producao');
        } catch (err) { toast(err.message, 'error'); }
      }
    };
  }

  if ($('#detail-add-ingredient')) {
    $('#detail-add-ingredient').onclick = () => {
      refreshIngredientSelects();
      if ($('#detail-ingredient-modal')) $('#detail-ingredient-modal').classList.remove('hidden');
    };
  }

  if ($('#detail-ingredient-form')) {
    $('#detail-ingredient-form').onsubmit = async e => {
      e.preventDefault();
      try {
        await api.adicionarIngredienteProducao(activeProductionId, {
          estoqueId: $('#detail-ingredient-select').value,
          quantidade: parseFloat($('#detail-ingredient-amount').value),
          temperatura: parseFloat($('#detail-ingredient-temp').value),
          tempo: parseInt($('#detail-ingredient-time').value)
        });
        $('#detail-ingredient-modal')?.classList.add('hidden');
        openProduction(activeProductionId);
        toast('Ingrediente adicionado!');
      } catch (err) { toast(err.message, 'error'); }
    };
  }

  if ($('#detail-ingredients-table')) {
    $('#detail-ingredients-table').onclick = async e => {
      const del = e.target.closest('[data-detail-remove]');
      if (del && confirm('Remover da produção?')) {
        try {
          await api.removerIngredienteProducao(activeProductionId, del.getAttribute('data-detail-remove'));
          openProduction(activeProductionId);
          toast('Removido!');
        } catch (err) { toast(err.message, 'error'); }
      }
    };
  }

  if ($('#stock-search')) $('#stock-search').oninput = () => { stockPage = 1; renderStock(); };
  if ($('#movement-filter')) $('#movement-filter').onchange = () => { movementPage = 1; renderMovements(); };
  if ($('#production-search')) $('#production-search').oninput = renderProductions;

  if ($('#stock-tabs')) {
    $('#stock-tabs').onclick = e => { const tab = e.target.closest('[data-category]'); if (tab) { $$('.category-tab').forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      currentCategory = tab.getAttribute('data-category');
      stockPage = 1;
      renderStock();
    }
    };
  }

  if ($('#recipe-date')) $('#recipe-date').value = dateISO();
  if ($('#stock-date')) $('#stock-date').value = dateISO();

  // Exibe a página de estoque na inicialização
  showPage('estoque');
});