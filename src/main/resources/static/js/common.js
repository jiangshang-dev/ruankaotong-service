const AUTH_KEY = "ruankao.admin.user";

const NAV = [
  { id: "dashboard", href: "./index.html", label: "概览", icon: "home" },
  { id: "clients", href: "./clients.html", label: "客户端 IP", icon: "ip" },
  { id: "qa", href: "./qa.html", label: "AI 问答", icon: "chat" },
  { id: "subjects", href: "./subjects.html", label: "学科管理", icon: "book" },
  { id: "prompts", href: "./prompts.html", label: "提示词", icon: "file" },
  { id: "settings", href: "./settings.html", label: "系统设置", icon: "gear" },
];

const ICONS = {
  home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-7H10v7H5a1 1 0 0 1-1-1z"/></svg>',
  ip: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/></svg>',
  chat: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 5h14a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H9l-4 3v-3H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z"/></svg>',
  book: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 0-3 3V4z"/></svg>',
  file: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M7 3h8l5 5v13a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z"/><path d="M15 3v6h6"/></svg>',
  gear: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.2a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.2a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9c.3.6.9 1 1.5 1H21a2 2 0 1 1 0 4h-.2a1.7 1.7 0 0 0-1.5 1z"/></svg>',
};

function currentUser() {
  try {
    return JSON.parse(sessionStorage.getItem(AUTH_KEY) || "null");
  } catch {
    return null;
  }
}

function isLoginPage() {
  return location.pathname.endsWith("/login.html") || location.pathname.endsWith("login.html");
}

window.Admin = {
  user: currentUser,
  login(account, name) {
    sessionStorage.setItem(AUTH_KEY, JSON.stringify({ account, name }));
  },
  logout() {
    sessionStorage.removeItem(AUTH_KEY);
    location.href = "./login.html";
  },
  toast(text) {
    let el = document.getElementById("toast");
    if (!el) {
      el = document.createElement("div");
      el.id = "toast";
      el.className = "toast";
      document.body.appendChild(el);
    }
    el.hidden = false;
    el.textContent = text;
    clearTimeout(this._t);
    this._t = setTimeout(() => {
      el.hidden = true;
    }, 2200);
  },
  qs(name) {
    return new URLSearchParams(location.search).get(name) || "";
  },
  esc(s) {
    return String(s ?? "")
      .replaceAll("&", "&amp;")
      .replaceAll("<", "&lt;")
      .replaceAll(">", "&gt;")
      .replaceAll('"', "&quot;");
  },
};

function wrapLayout() {
  const page = document.getElementById("page");
  if (!page) return;
  const nav = page.dataset.nav || "";
  const title = page.dataset.title || "管理后台";
  const inner = page.innerHTML;
  const user = currentUser();
  document.getElementById("app").innerHTML = `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <div class="brand-logo">软</div>
          <div>
            <h2>软考通</h2>
            <small>管理后台</small>
          </div>
        </div>
        <nav class="nav">
          ${NAV.map((item) => `
            <a href="${item.href}" class="${item.id === nav ? "active" : ""}">
              ${ICONS[item.icon]} ${item.label}
            </a>
          `).join("")}
        </nav>
        <div class="sidebar-foot">
          <span>${Admin.esc(user?.name || "未登录")}</span>
          <button class="btn ghost sm" type="button" id="logout">退出</button>
        </div>
      </aside>
      <section class="main">
        <header class="topbar">
          <h1>${Admin.esc(title)}</h1>
          <span class="demo-tag">管理端需登录 · 客户端只记 IP</span>
        </header>
        <div class="content">
          <div class="banner">桌面客户端无需登录。问答按客户端 IP 落库；本页数据为示例，保存不会写入后端。</div>
          ${inner}
        </div>
      </section>
    </div>
  `;
  document.getElementById("logout")?.addEventListener("click", () => Admin.logout());
}

document.addEventListener("DOMContentLoaded", () => {
  if (isLoginPage()) {
    if (currentUser()) location.href = "./index.html";
    return;
  }
  if (!currentUser()) {
    location.href = "./login.html";
    return;
  }
  wrapLayout();
  document.dispatchEvent(new CustomEvent("admin:ready"));
});
