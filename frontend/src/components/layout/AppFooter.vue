<template>
  <footer class="app-footer">
    <div class="footer-content">
      <nav class="footer-links">
        <a href="#">About</a>
        <a href="#">Privacy</a>
        <a href="#">Terms</a>
        <a href="#">Contact</a>
      </nav>
      <span class="footer-copy">— © 2025 Problem.io</span>
    </div>
  </footer>
  </template>
  
  <script setup lang="ts">
  </script>
  
  <style scoped>
.app-footer {
  position: fixed;
  bottom: 12px;
  left: 20px;
  z-index: 50;
  font-size: 0.75rem;
  color: var(--color-text-muted);
  opacity: 0.8;
  pointer-events: auto;
}

.footer-content {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.footer-links {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.footer-links a {
  text-decoration: none;
  color: var(--color-text-muted);
}

.footer-links a:hover {
  text-decoration: underline;
}

.footer-copy {
  color: var(--color-text-muted);
}

@media (max-width: 768px) {
  .app-footer {
    font-size: 0.65rem;
    bottom: 8px;
    left: 12px;
  }
}
  </style>
