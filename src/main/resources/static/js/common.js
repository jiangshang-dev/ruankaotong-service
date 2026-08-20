const TOKEN_KEY = "ruankao.admin.token";
const USER_KEY = "ruankao.admin.user";

const NAV = [
  { id: "dashboard", href: "./index.html", label: "概览", icon: "home" },
  { id: "users", href: "./users.html", label: "用户管理", icon: "users" },
  { id: "clients", href: "./clients.html", label: "客户端 IP", icon: "ip" },
  { id: "qa", href: "./qa.html", label: "AI 问答", icon: "chat" },
  { id: "subjects", href: "./subjects.html", label: "学科管理", icon: "book" },
  { id: "settings", href: "./settings.html", label: "系统设置", icon: "gear" },
];

const ICONS = {
  home: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 10.5 12 4l8 6.5V20a1 1 0 0 1-1 1h-5v-7H10v7H5a1 1 0 0 1-1-1z"/></svg>',
  ip: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3a14 14 0 0 1 0 18M12 3a14 14 0 0 0 0 18"/></svg>',
  chat: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 5h14a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H9l-4 3v-3H5a1 1 0 0 1-1-1V6a1 1 0 0 1 1-1z"/></svg>',
  users: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/></svg>',
  book: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M5 4h11a3 3 0 0 1 3 3v13H8a3 3 0 0 0-3 3V4z"/></svg>',
  gear: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.2a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.2a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.2a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9c.3.6.9 1 1.5 1H21a2 2 0 1 1 0 4h-.2a1.7 1.7 0 0 0-1.5 1z"/></svg>',
};

function isLoginPage() {
  return location.pathname.endsWith("/login.html") || location.pathname.endsWith("login.html");
}

function token() {
  return sessionStorage.getItem(TOKEN_KEY) || "";
}

function currentUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY) || "null");
  } catch {
    return null;
  }
}

window.Admin = {
  user: currentUser,
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
  fmt(v) {
    if (!v) return "";
    return String(v).replace("T", " ").replace(/\.\d+/, "").slice(0, 19);
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
  saveLogin(data) {
    sessionStorage.setItem(TOKEN_KEY, data.token || "");
    sessionStorage.setItem(USER_KEY, JSON.stringify({ account: data.account, name: data.name }));
  },
  async logout() {
    try {
      await fetch("/api/admin/logout", {
        method: "POST",
        headers: { Authorization: "Bearer " + token() },
      });
    } catch {
      // ignore
    }
    sessionStorage.removeItem(TOKEN_KEY);
    sessionStorage.removeItem(USER_KEY);
    location.href = "./login.html";
  },
  async api(path, options = {}) {
    const headers = { ...(options.headers || {}) };
    if (token()) headers.Authorization = "Bearer " + token();
    if (options.body && !headers["Content-Type"] && !(options.body instanceof FormData)) {
      headers["Content-Type"] = "application/json";
    }
    const res = await fetch(path, { ...options, headers });
    const data = await res.json().catch(() => ({}));
    if (res.status === 401) {
      sessionStorage.removeItem(TOKEN_KEY);
      sessionStorage.removeItem(USER_KEY);
      if (!isLoginPage()) location.href = "./login.html";
      throw new Error(data.message || "请先登录");
    }
    if (!res.ok) {
      throw new Error(data.message || "请求失败");
    }
    return data;
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
          <span class="demo-tag">管理端需登录 · AI 记录按登录用户隔离</span>
        </header>
        <div class="content">${inner}</div>
      </section>
    </div>
  `;
  document.getElementById("logout")?.addEventListener("click", () => Admin.logout());
}

document.addEventListener("DOMContentLoaded", async () => {
  if (isLoginPage()) {
    if (token()) {
      try {
        await Admin.api("/api/admin/me");
        location.href = "./index.html";
        return;
      } catch {
        sessionStorage.removeItem(TOKEN_KEY);
        sessionStorage.removeItem(USER_KEY);
      }
    }
    return;
  }
  if (!token()) {
    location.href = "./login.html";
    return;
  }
  try {
    const me = await Admin.api("/api/admin/me");
    sessionStorage.setItem(USER_KEY, JSON.stringify({ account: me.account, name: me.name }));
  } catch {
    return;
  }
  wrapLayout();
  document.dispatchEvent(new CustomEvent("admin:ready"));
});
