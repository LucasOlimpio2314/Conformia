// Comportamentos de tela do Conformia. Só interface: toda regra é validada no servidor.
(() => {
  const $ = (s, el = document) => el.querySelector(s);
  const $$ = (s, el = document) => [...el.querySelectorAll(s)];

  // ---------- aviso rápido (toast) ----------
  function toast(msg) {
    const t = $("#toast");
    if (!t || !msg) return;
    t.textContent = msg;
    t.classList.add("show");
    clearTimeout(t._h);
    t._h = setTimeout(() => t.classList.remove("show"), 2800);
  }

  // ---------- fetch com token CSRF (Spring Security exige em POST) ----------
  // FormData vira URLSearchParams (x-www-form-urlencoded): enviado como multipart, o Tomcat 11
  // recusa (413) formulários com muitas partes — o do risco tem ~100 (um marcador por checkbox).
  function postar(url, body) {
    const headers = { "X-Requested-With": "fetch" };
    const token = $('meta[name="_csrf"]'), header = $('meta[name="_csrf_header"]');
    if (token && header) headers[header.content] = token.content;
    const corpo = body instanceof FormData ? new URLSearchParams(body) : body;
    return fetch(url, { method: "POST", body: corpo, headers });
  }

  // ---------- mostrar/ocultar senha ----------
  document.addEventListener("click", e => {
    const b = e.target.closest("[data-toggle]");
    if (!b) return;
    const input = document.getElementById(b.dataset.toggle);
    const mostrar = input.type === "password";
    input.type = mostrar ? "text" : "password";
    b.textContent = mostrar ? "Ocultar" : "Mostrar";
  });

  // ---------- login: "esqueci a senha" ----------
  const esqueci = $("[data-esqueci]");
  if (esqueci) esqueci.addEventListener("click", () => {
    $("#login-note").innerHTML = "Esqueceu a senha? Peça a um <b>Gerente</b> para gerar uma senha temporária. Com ela você entra e cria uma nova.";
  });

  // ---------- requisitos de senha marcados ao vivo ----------
  $$("[data-regras-senha]").forEach(form => {
    const nova = $("#novaSenha", form), conf = $("#confirmacao", form);
    const atualizar = () => {
      const n = nova.value, ok = {
        tam: n.length >= 8 && n.length <= 64,
        mix: /\p{L}/u.test(n) && /\d/.test(n),
        eq: n.length > 0 && n === conf.value
      };
      $$(".rules li", form).forEach(li => li.classList.toggle("ok", !!ok[li.dataset.r]));
    };
    nova.addEventListener("input", atualizar);
    conf.addEventListener("input", atualizar);
  });

  // ---------- lista de usuários: filtros ----------
  const tabela = $("#tabela-usuarios");
  if (tabela) {
    let perfil = "";
    const filtrar = () => {
      const q = $("#busca").value.trim().toLowerCase(), sit = $("#filtro-situacao").value;
      const linhas = $$("tbody tr[data-perfil]", tabela);
      let visiveis = 0;
      linhas.forEach(tr => {
        const ok = (!perfil || tr.dataset.perfil === perfil)
          && (!sit || (sit === "ativo") === (tr.dataset.ativo === "true"))
          && (!q || tr.dataset.busca.includes(q));
        tr.hidden = !ok;
        if (ok) visiveis++;
      });
      $("#sem-resultado").hidden = visiveis > 0;
      $("#contador").textContent = `${visiveis} de ${linhas.length} usuários`;
    };
    $$(".chip").forEach(c => c.addEventListener("click", () => {
      perfil = c.dataset.perfil;
      $$(".chip").forEach(x => x.setAttribute("aria-pressed", x === c));
      filtrar();
    }));
    $("#busca").addEventListener("input", filtrar);
    $("#filtro-situacao").addEventListener("change", filtrar);
  }

  // ---------- drawer (cadastro) ----------
  const drawer = $("#drawer"), scrim = $("#scrim");
  let origemFoco = null;

  async function abrirDrawer(url, origem) {
    origemFoco = origem;
    const resp = await fetch(url, { headers: { "X-Requested-With": "fetch" } });
    if (!resp.ok) { toast(await resp.text() || "Não foi possível abrir o formulário."); return; }
    drawer.innerHTML = await resp.text();
    prepararFormAtivo();
    prepararFormRisco();
    prepararFormsPlanoAceite();
    drawer.classList.add("open");
    scrim.classList.add("open");
    drawer.setAttribute("aria-hidden", "false");
    // foco no primeiro campo de digitação (o seletor combinado devolve o primeiro na ordem da página;
    // checkbox/radio ficam de fora — no aceite o 1º input é a declaração, e o texto iria "para o nada")
    setTimeout(() => {
      const f = $("input:not([type=hidden]):not([type=checkbox]):not([type=radio]), textarea, select", drawer);
      if (f) f.focus();
    }, 200);
  }
  function fecharDrawer() {
    drawer.classList.remove("open");
    scrim.classList.remove("open");
    drawer.setAttribute("aria-hidden", "true");
    if (origemFoco) origemFoco.focus();
  }

  if (drawer) {
    // delegação: botões "Editar" de cada linha também abrem o drawer
    document.addEventListener("click", e => {
      const b = e.target.closest("[data-drawer-url]");
      if (b) abrirDrawer(b.dataset.drawerUrl, b);
    });
    scrim.addEventListener("click", fecharDrawer);
    drawer.addEventListener("click", e => { if (e.target.closest("[data-drawer-fechar]")) fecharDrawer(); });
    drawer.addEventListener("change", prepararFormAtivo);
    drawer.addEventListener("change", () => prepararFormRisco());
    drawer.addEventListener("change", () => prepararFormsPlanoAceite());

    // envio do formulário:
    // 422 = volta o form com erros; X-Redirect = salvou, recarrega; 200 = senha temporária (usuários)
    drawer.addEventListener("submit", async e => {
      const form = e.target.closest("[data-form-drawer]");
      if (!form) return;
      e.preventDefault();
      const botao = $("button[type=submit]", form);
      botao.disabled = true;
      try {
        const resp = await postar(form.action, new FormData(form));
        const html = await resp.text();
        if (resp.status === 422) {
          drawer.innerHTML = html;
          prepararFormAtivo();
          prepararFormRisco();
          prepararFormsPlanoAceite();
          const erro = $(".invalid input, .invalid select, .alert", drawer);
          if (erro) erro.focus?.();
          return;
        }
        if (!resp.ok) { toast(html || "Não foi possível salvar. Tente de novo."); return; }
        const destino = resp.headers.get("X-Redirect");
        if (destino) { location.href = destino; return; }
        fecharDrawer();
        mostrarSenha(html);
      } finally {
        botao.disabled = false;
      }
    });
  }

  // ---------- inventário: comportamento do formulário do ativo ----------
  const ROTULO_CRITICIDADE = { 1: "Muito baixa", 2: "Baixa", 3: "Média", 4: "Alta", 5: "Muito alta" };
  function prepararFormAtivo() {
    const form = drawer && $("[data-form-ativo]", drawer);
    if (!form) return;
    // fornecedor só aparece (e só é exigido no servidor) se for terceirizado
    const terc = $("[data-terceirizado]", form);
    $("[data-campo-fornecedor]", form).hidden = !terc.checked;
    // criticidade = maior nota entre C, I e D
    const nota = n => +($(`input[name="${n}"]:checked`, form)?.value || 0);
    const k = Math.max(nota("confidencialidade"), nota("integridade"), nota("disponibilidade"));
    $("[data-criticidade-calc]", form).textContent = k ? `${k} · ${ROTULO_CRITICIDADE[k]}` : "—";
    // explica a classificação escolhida
    const cls = $('input[name="classificacao"]:checked', form);
    $("[data-classificacao-hint]", form).textContent = cls ? cls.closest("label").dataset.descricao : "";
  }

  // ---------- inventário: filtros da lista ----------
  const tabelaAtivos = $("#tabela-ativos");
  if (tabelaAtivos) {
    let tipo = "";
    const filtrar = () => {
      const q = $("#busca").value.trim().toLowerCase();
      const cls = $("#filtro-classificacao").value, sit = $("#filtro-situacao-ativo").value;
      const linhas = $$("tbody tr[data-tipo]", tabelaAtivos);
      let visiveis = 0;
      linhas.forEach(tr => {
        const ok = (!tipo || tr.dataset.tipo === tipo) && (!cls || tr.dataset.classificacao === cls)
          && (!sit || tr.dataset.situacao === sit) && (!q || tr.dataset.busca.includes(q));
        tr.hidden = !ok;
        if (ok) visiveis++;
      });
      $("#sem-resultado").hidden = visiveis > 0;
      $("#contador").textContent = `${visiveis} ativos exibidos`;
    };
    $$("[data-filtro-tipo]").forEach(c => c.addEventListener("click", () => {
      tipo = c.dataset.filtroTipo;
      $$("[data-filtro-tipo]").forEach(x => x.setAttribute("aria-pressed", x === c));
      filtrar();
    }));
    $("#busca").addEventListener("input", filtrar);
    $("#filtro-classificacao").addEventListener("change", filtrar);
    $("#filtro-situacao-ativo").addEventListener("change", filtrar);
    filtrar(); // começa mostrando só os ativos em uso
  }

  // ---------- modal de justificativa (descontinuar ativo, encerrar risco) ----------
  // Botão com data-justificar-url abre o modal; sucesso = X-Redirect; erro = texto (422) no campo.
  const modalJust = $("#modal-justificativa");
  if (modalJust) {
    const form = $("form", modalJust), campo = $("[data-justificar-campo]", modalJust);
    let url = null;
    const fechar = () => { modalJust.hidden = true; };
    document.addEventListener("click", e => {
      const b = e.target.closest("[data-justificar-url]");
      if (b) {
        url = b.dataset.justificarUrl;
        $("[data-justificar-alvo]", modalJust).textContent = b.dataset.justificarCodigo;
        form.reset();
        campo.classList.remove("invalid");
        $("[data-justificar-erro]", modalJust).textContent = "";
        modalJust.hidden = false;
        $("textarea", modalJust).focus();
      }
      if (e.target.closest("[data-justificar-fechar]")) fechar();
    });
    document.addEventListener("keydown", e => { if (e.key === "Escape" && !modalJust.hidden) fechar(); });
    form.addEventListener("submit", async e => {
      e.preventDefault();
      const resp = await postar(url, new FormData(form));
      const destino = resp.headers.get("X-Redirect");
      if (resp.ok && destino) { location.href = destino; return; }
      campo.classList.add("invalid");
      $("[data-justificar-erro]", modalJust).textContent = await resp.text();
    });
  }

  // ---------- riscos: filtros da lista (cards de nível + select) ----------
  const tabelaRiscos = $("#tabela-riscos");
  if (tabelaRiscos) {
    const selNivel = $("#filtro-nivel"), selSit = $("#filtro-situacao-risco");
    const filtrar = () => {
      const q = $("#busca").value.trim().toLowerCase(), nivel = selNivel.value, sit = selSit.value;
      const linhas = $$("tbody tr[data-nivel]", tabelaRiscos);
      let visiveis = 0;
      linhas.forEach(tr => {
        const ok = (!nivel || tr.dataset.nivel === nivel) && (!sit || tr.dataset.situacao === sit)
          && (!q || tr.dataset.busca.includes(q));
        tr.hidden = !ok;
        if (ok) visiveis++;
      });
      $$("[data-filtro-nivel]").forEach(c => c.setAttribute("aria-pressed", c.dataset.filtroNivel === nivel));
      $("#sem-resultado").hidden = visiveis > 0;
      $("#contador").textContent = `${visiveis} riscos exibidos`;
    };
    $$("[data-filtro-nivel]").forEach(c => c.addEventListener("click", () => {
      selNivel.value = selNivel.value === c.dataset.filtroNivel ? "" : c.dataset.filtroNivel;
      filtrar();
    }));
    $("#busca").addEventListener("input", filtrar);
    selNivel.addEventListener("change", filtrar);
    selSit.addEventListener("change", filtrar);
    filtrar(); // começa mostrando só os riscos ativos
  }

  // ---------- nível = P × I (formulário de risco e de residual) ----------
  // As faixas vêm do servidor (enum NivelRisco, em data-niveis): a regra não é repetida aqui.
  function atualizarNivel(form) {
    const resultado = $("[data-resultado]", form);
    if (!resultado) return;
    const nota = n => +($(`input[name="${n}"]:checked`, form)?.value || 0);
    const faixas = resultado.dataset.niveis.split(";").map(s => s.split(":"));
    const p = nota("probabilidade"), i = nota("impacto"), v = p * i;
    const faixa = v ? faixas.find(f => v >= +f[1]) : null;
    $("[data-resultado-calc]", form).textContent = v ? `${p} × ${i} = ${v}` : "Escolha probabilidade e impacto";
    const etiqueta = $("[data-resultado-nivel]", form);
    etiqueta.className = "nivel" + (faixa ? " n-" + faixa[0] : "");
    etiqueta.textContent = faixa ? faixa[2] : "—";
  }

  // ---------- plano e aceite: comportamento dos formulários ----------
  function prepararFormsPlanoAceite() {
    if (!drawer) return;
    const residual = $("[data-form-residual]", drawer);
    if (residual) atualizarNivel(residual);
    // aceite: o botão só libera com a declaração marcada
    const aceite = $("[data-form-aceite]", drawer);
    if (aceite) $("[data-confirmar-aceite]", aceite).disabled = !$("[data-declaracao]", aceite).checked;
    // ação: destaca que a evidência é obrigatória quando a situação é "Concluída"
    const acao = $("[data-form-acao]", drawer);
    if (acao) {
      const concluir = $("[data-situacao-acao]", acao).value === "CONCLUIDA";
      $("[data-evidencia-hint]", acao).classList.toggle("warn-text", concluir);
      $("[data-evidencia-hint]", acao).textContent = concluir ? "(obrigatória para concluir)" : "(obrigatória ao concluir)";
    }
  }

  // ---------- plano: filtros (situação, responsável, busca) ----------
  const planos = $("#planos");
  if (planos && $("#filtro-situacao-acao")) {
    const filtrar = () => {
      const q = $("#busca").value.trim().toLowerCase();
      const sit = $("#filtro-situacao-acao").value, resp = $("#filtro-responsavel").value;
      const ativo = q || sit || resp;
      let algum = false;
      $$("article.plano", planos).forEach(card => {
        let visiveis = 0;
        $$("tbody tr[data-situacao]", card).forEach(tr => {
          const s = tr.dataset.situacao;
          const okSit = !sit || (sit === "ABERTAS" ? (s === "PENDENTE" || s === "EM_ANDAMENTO")
            : sit === "ATRASADAS" ? tr.dataset.atrasada === "true" : s === sit);
          const ok = okSit && (!resp || tr.dataset.responsavel === resp) && (!q || tr.dataset.busca.includes(q));
          tr.hidden = !ok;
          if (ok) visiveis++;
        });
        // card aparece se tem ação visível, ou (só com busca) se o próprio risco bate
        const riscoBate = q && !sit && !resp && card.dataset.buscaRisco.includes(q);
        card.hidden = ativo && visiveis === 0 && !riscoBate;
        if (!card.hidden) algum = true;
      });
      $("#sem-resultado").hidden = algum || !ativo;
    };
    $("#busca").addEventListener("input", filtrar);
    $("#filtro-situacao-acao").addEventListener("change", filtrar);
    $("#filtro-responsavel").addEventListener("change", filtrar);
  }

  // ---------- riscos: comportamento do formulário ----------
  const ROTULO_IMPACTO = { 1: "Insignificante", 2: "Menor", 3: "Moderado", 4: "Maior", 5: "Catastrófico" };
  function prepararFormRisco() {
    const form = drawer && $("[data-form-risco]", drawer);
    if (!form) return;
    atualizarNivel(form);

    // sugestão de impacto = maior criticidade entre os ativos marcados
    const marcados = $$("#lista-ativos input:checked", form);
    const sugestao = $("[data-sugestao]", form);
    if (marcados.length) {
      const maior = marcados.reduce((m, x) => +x.dataset.criticidade > +m.dataset.criticidade ? x : m);
      const crit = +maior.dataset.criticidade;
      sugestao.hidden = false;
      $("[data-sugestao-texto]", form).textContent =
        `Sugestão: impacto ${crit} (${ROTULO_IMPACTO[crit]}) — maior criticidade entre os ativos (${maior.dataset.codigo}).`;
      $("[data-sugestao-usar]", form).onclick = () => {
        $(`input[name="impacto"][value="${crit}"]`, form).checked = true;
        prepararFormRisco();
      };
      // dono sugerido: proprietário do primeiro ativo, se ainda não escolhido e se estiver na lista
      const dono = $("#donoId", form);
      if (!dono.value && $(`option[value="${marcados[0].dataset.proprietario}"]`, dono)) dono.value = marcados[0].dataset.proprietario;
    } else {
      sugestao.hidden = true;
    }

    // controles escolhidos viram etiquetas acima da lista
    $("[data-controles-escolhidos]", form).innerHTML = $$("#lista-controles input:checked", form)
      .map(c => `<span class="ctrl">${c.dataset.rotulo.replace(/[&<>"]/g, "")}</span>`).join("");

    // tratamento: "A definir" quando nada escolhido + explicação da opção
    if (!$('input[name="tratamento"]:checked', form)) $('input[name="tratamento"][value=""]', form).checked = true;
    $("[data-tratamento-hint]", form).textContent = $('input[name="tratamento"]:checked', form).closest("label").dataset.descricao;
  }

  if (drawer) {
    // filtro dos seletores (ativos e controles); marcados sempre aparecem
    drawer.addEventListener("input", e => {
      const busca = e.target.closest("[data-filtra]");
      if (!busca) return;
      const q = busca.value.trim().toLowerCase(), lista = $(busca.dataset.filtra, drawer);
      $$("li[data-busca]", lista).forEach(li =>
        li.hidden = !!q && !li.dataset.busca.includes(q) && !$("input", li).checked);
      $$("li.grp-title", lista).forEach(t =>
        t.hidden = !$$(`li[data-busca][data-grupo="${t.dataset.grupo}"]`, lista).some(li => !li.hidden));
    });

    // "+ Nova" ameaça/vulnerabilidade: cadastra no catálogo e já seleciona
    drawer.addEventListener("click", async e => {
      const abrir = e.target.closest("[data-catalogo-abrir]");
      if (abrir) {
        const caixa = $(`[data-catalogo="${abrir.dataset.catalogoAbrir}"]`, drawer);
        caixa.hidden = !caixa.hidden;
        if (!caixa.hidden) $("input", caixa).focus();
        return;
      }
      const caixa = e.target.closest("[data-catalogo]");
      if (!caixa || !e.target.closest("button")) return;
      const tipo = caixa.dataset.catalogo, form = $("[data-form-risco]", drawer);
      const url = tipo === "ameaca" ? form.dataset.urlAmeaca : form.dataset.urlVulnerabilidade;
      const corpo = new FormData();
      corpo.append("nome", $("input", caixa).value);
      const resp = await postar(url, corpo);
      const json = await resp.json();
      const erro = $(`[data-catalogo-erro="${tipo}"]`, drawer);
      if (!resp.ok) { erro.textContent = json.mensagem; caixa.closest(".field").classList.add("invalid"); return; }
      const select = $(tipo === "ameaca" ? "#ameacaId" : "#vulnerabilidadeId", drawer);
      select.add(new Option(json.nome, json.id, true, true));
      erro.textContent = "";
      caixa.closest(".field").classList.remove("invalid");
      $("input", caixa).value = "";
      caixa.hidden = true;
      toast(`"${json.nome}" adicionada ao catálogo`);
    });
  }

  // ---------- modal: redefinir senha / mostrar senha temporária ----------
  const modal = $("#modal");
  let urlReset = null;

  function mostrarSenha(html) {
    $("#modal-confirmar").hidden = true;
    const r = $("#modal-resultado");
    r.innerHTML = html;
    r.hidden = false;
    modal.hidden = false;
    $("[data-copiar]", r).focus();
  }
  function fecharModal() { modal.hidden = true; }

  if (modal) {
    document.addEventListener("click", e => {
      const b = e.target.closest("[data-reset-url]");
      if (!b) return;
      urlReset = b.dataset.resetUrl;
      $("#modal-nome").textContent = b.dataset.resetNome;
      $("#modal-confirmar").hidden = false;
      $("#modal-resultado").hidden = true;
      modal.hidden = false;
      $("#modal-gerar").focus();
    });

    $("#modal-gerar").addEventListener("click", async () => {
      const resp = await postar(urlReset);
      const html = await resp.text();
      if (!resp.ok) { fecharModal(); toast(html); return; }
      mostrarSenha(html);
    });

    modal.addEventListener("click", async e => {
      if (e.target.closest("[data-modal-fechar]")) fecharModal();
      const copiar = e.target.closest("[data-copiar]");
      if (copiar) {
        try {
          await navigator.clipboard.writeText($("code", modal).textContent);
          copiar.textContent = "Copiado";
        } catch {
          copiar.textContent = "Selecione e copie";
        }
      }
      // depois de anotar a senha, recarrega a lista (sem a senha na página)
      if (e.target.closest("[data-recarregar]")) {
        location.href = $("[data-senha-gerada]", modal).dataset.recarregarUrl;
      }
    });
  }

  document.addEventListener("keydown", e => {
    if (e.key !== "Escape") return;
    if (modal && !modal.hidden && $("#modal-resultado").hidden) fecharModal();
    else if (drawer && drawer.classList.contains("open")) fecharDrawer();
  });

  // mensagem vinda do servidor após redirect (flash attribute "toast")
  const t = $("#toast");
  if (t && t.dataset.msg) toast(t.dataset.msg);
})();
