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
  function postar(url, body) {
    const headers = { "X-Requested-With": "fetch" };
    const token = $('meta[name="_csrf"]'), header = $('meta[name="_csrf_header"]');
    if (token && header) headers[header.content] = token.content;
    return fetch(url, { method: "POST", body, headers });
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
    drawer.innerHTML = await resp.text();
    drawer.classList.add("open");
    scrim.classList.add("open");
    drawer.setAttribute("aria-hidden", "false");
    setTimeout(() => { const f = $("input:not([type=hidden])", drawer); if (f) f.focus(); }, 200);
  }
  function fecharDrawer() {
    drawer.classList.remove("open");
    scrim.classList.remove("open");
    drawer.setAttribute("aria-hidden", "true");
    if (origemFoco) origemFoco.focus();
  }

  if (drawer) {
    $$("[data-drawer-url]").forEach(b => b.addEventListener("click", () => abrirDrawer(b.dataset.drawerUrl, b)));
    scrim.addEventListener("click", fecharDrawer);
    drawer.addEventListener("click", e => { if (e.target.closest("[data-drawer-fechar]")) fecharDrawer(); });

    // envio do formulário: 422 = volta o form com erros; 200 = senha temporária gerada
    drawer.addEventListener("submit", async e => {
      const form = e.target.closest("[data-form-usuario]");
      if (!form) return;
      e.preventDefault();
      const botao = $("button[type=submit]", form);
      botao.disabled = true;
      try {
        const resp = await postar(form.action, new FormData(form));
        const html = await resp.text();
        if (resp.status === 422) {
          drawer.innerHTML = html;
          const erro = $(".invalid input, .alert", drawer);
          if (erro) erro.focus?.();
          return;
        }
        if (!resp.ok) { toast(html || "Não foi possível salvar. Tente de novo."); return; }
        fecharDrawer();
        mostrarSenha(html);
      } finally {
        botao.disabled = false;
      }
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
