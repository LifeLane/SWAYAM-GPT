/**
 * SWAYAM-GPT Public Website Core Interactions
 * Lightweight, accessible vanilla JavaScript
 */
document.addEventListener('DOMContentLoaded', () => {
  initThemeToggle();
  initMobileNav();
  initInteractiveFlow();
  initCopyHelpers();
});

/* Theme Toggle (Dark / Light) */
function initThemeToggle() {
  const themeToggles = document.querySelectorAll('.theme-toggle');
  const storedTheme = localStorage.getItem('swayam_theme');
  const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
  
  const currentTheme = storedTheme || (prefersDark ? 'dark' : 'dark');
  document.documentElement.setAttribute('data-theme', currentTheme);
  updateThemeIcons(currentTheme);

  themeToggles.forEach(toggle => {
    toggle.addEventListener('click', () => {
      const activeTheme = document.documentElement.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
      document.documentElement.setAttribute('data-theme', activeTheme);
      localStorage.setItem('swayam_theme', activeTheme);
      updateThemeIcons(activeTheme);
    });
  });
}

function updateThemeIcons(theme) {
  const sunIcons = document.querySelectorAll('.icon-sun');
  const moonIcons = document.querySelectorAll('.icon-moon');
  
  if (theme === 'light') {
    sunIcons.forEach(i => i.style.display = 'none');
    moonIcons.forEach(i => i.style.display = 'block');
  } else {
    sunIcons.forEach(i => i.style.display = 'block');
    moonIcons.forEach(i => i.style.display = 'none');
  }
}

/* Mobile Navigation Drawer */
function initMobileNav() {
  const menuBtn = document.querySelector('.mobile-menu-toggle');
  const mobileNav = document.querySelector('.mobile-nav');
  const navLinks = document.querySelectorAll('.mobile-nav-link');

  if (!menuBtn || !mobileNav) return;

  menuBtn.addEventListener('click', () => {
    const isOpen = mobileNav.classList.toggle('open');
    menuBtn.setAttribute('aria-expanded', isOpen.toString());
  });

  navLinks.forEach(link => {
    link.addEventListener('click', () => {
      mobileNav.classList.remove('open');
      menuBtn.setAttribute('aria-expanded', 'false');
    });
  });

  // Close on outside click
  document.addEventListener('click', (e) => {
    if (!mobileNav.contains(e.target) && !menuBtn.contains(e.target) && mobileNav.classList.contains('open')) {
      mobileNav.classList.remove('open');
      menuBtn.setAttribute('aria-expanded', 'false');
    }
  });

  // Close on Escape key
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && mobileNav.classList.contains('open')) {
      mobileNav.classList.remove('open');
      menuBtn.setAttribute('aria-expanded', 'false');
    }
  });
}

/* Interactive Architecture Sandbox in Hero */
function initInteractiveFlow() {
  const modeButtons = document.querySelectorAll('[data-routing-mode]');
  const statusIndicator = document.getElementById('demo-routing-status');
  const latencyBadge = document.getElementById('demo-latency-badge');
  const providerTitle = document.getElementById('demo-provider-title');
  const providerDesc = document.getElementById('demo-provider-desc');
  const encryptionBadge = document.getElementById('demo-encryption-badge');

  if (!modeButtons.length || !statusIndicator) return;

  const modeConfigs = {
    local: {
      status: 'On-Device Edge LiteRT',
      latency: '~12ms Local NPU/GPU',
      provider: 'LiteRT-LM On-Device Model',
      desc: 'Zero network egress. Inferences computed strictly inside device neural runtime.',
      encryption: 'Hardware AES-256-GCM Locked'
    },
    server: {
      status: 'Private LAN Cluster',
      latency: '~45ms Private HTTPS',
      provider: 'Self-Hosted Sovereign Gateway',
      desc: 'Encrypted traffic routed to your private server without Big Tech cloud telemetry.',
      encryption: 'TLS 1.3 + Vault Envelope'
    },
    cloud: {
      status: 'Google Gemini 2.5 Flash',
      latency: '~380ms Cloud Fallback',
      provider: 'Google Gemini API (User Opt-in)',
      desc: 'High-capability cloud reasoning with strict permission gating and audit logs.',
      encryption: 'End-to-End TLS Encrypted'
    },
    offline: {
      status: 'Air-Gapped Offline Mode',
      latency: '0ms Network Blocked',
      provider: 'Enforced Local Sovereign Sandbox',
      desc: 'All network sockets strictly disabled by PrivacyEngine policy layer.',
      encryption: 'AndroidKeyStore Enforced'
    }
  };

  modeButtons.forEach(btn => {
    btn.addEventListener('click', () => {
      modeButtons.forEach(b => {
        b.classList.remove('active');
        b.style.borderColor = 'var(--border-subtle)';
      });
      btn.classList.add('active');
      btn.style.borderColor = 'var(--accent-primary)';

      const mode = btn.getAttribute('data-routing-mode');
      const cfg = modeConfigs[mode] || modeConfigs.local;

      if (statusIndicator) statusIndicator.textContent = cfg.status;
      if (latencyBadge) latencyBadge.textContent = cfg.latency;
      if (providerTitle) providerTitle.textContent = cfg.provider;
      if (providerDesc) providerDesc.textContent = cfg.desc;
      if (encryptionBadge) encryptionBadge.textContent = cfg.encryption;
    });
  });
}

/* Copy to Clipboard Helpers */
function initCopyHelpers() {
  const copyButtons = document.querySelectorAll('[data-copy-text]');
  copyButtons.forEach(btn => {
    btn.addEventListener('click', async () => {
      const text = btn.getAttribute('data-copy-text');
      try {
        await navigator.clipboard.writeText(text);
        const origText = btn.textContent;
        btn.textContent = 'Copied!';
        setTimeout(() => {
          btn.textContent = origText;
        }, 2000);
      } catch (err) {
        console.error('Clipboard copy failed', err);
      }
    });
  });
}
