const withS3Public = (path) => {
  if (!path) return path;
  const clean = path.startsWith("/") ? path.slice(1) : path;
  return `/uploads/${clean}`;
};

export const POPOVER_DECORATIONS = {
  default: {
    name: "Default",
    style: { backgroundColor: "var(--color-bg-card)", color: "var(--color-text-main)" },
    textColor: "var(--color-text-main)",
  },
  cybercity: {
    name: "Cybercity",
    image: withS3Public("public/popover/cybercity.jpg"),
    textColor: "#ffffff",
    textStyle: {
      textShadow: "0 0 5px #00eaff, 0 0 10px #00eaff",
      WebkitTextStroke: "1px #00eaff",
      animation: "neon-pulse 2s infinite ease-in-out",
    },
    buttonStyle: {
      backgroundColor: "rgba(0, 0, 0, 0.7)",
      borderWidth: "2px",
      borderStyle: "solid",
      color: "#ffffff",
      animation: "border-flow 4s infinite linear",
      transition: "all 0.3s ease",
    },
  },
  neon_cyber: {
    name: "Neon Cyber",
    image: withS3Public("public/popover/cyberpunk.jpg"),
    textColor: "#00ff00",
    style: {
      backgroundColor: "#0a0a0a",
      border: "2px solid #00ff00",
      boxShadow: "0 0 15px #00ff00",
      "--neon-color": "#00ff00",
    },
    textStyle: {
      background: "#00ff00",
      animation: "text-neon-pulse 1.5s infinite alternate",
    },
    buttonStyle: {
      background: "rgba(0,255,0,0.1)",
      boxShadow: "0 0 5px #00ff00",
    },
  },
  gold_rush: {
    name: "Gold Rush",
    textColor: "#4a3b0f",
    style: {
      background: "linear-gradient(to right, #bf953f, #fcf6ba, #b38728, #fbf5b7, #aa771c)",
      backgroundSize: "200% auto",
      animation: "gradient-move 3s linear infinite",
      border: "1px solid #b38728",
    },
    buttonStyle: {
      background: "rgba(255,255,255,0.4)",
      borderColor: "#4a3b0f",
    },
  },
  glitch_error: {
    name: "Glitch Error",
    textColor: "#000",
    style: {
      backgroundColor: "#ffffff",
      border: "2px solid #000",
      boxShadow: "6px 6px 0px #000",
      animation: "glitch-skew 2s infinite steps(2, end)",
    },
    buttonStyle: {
      background: "#ff0000",
      color: "white",
      border: "none",
    },
  },
  rainbow_pop: {
    name: "Rainbow Pop",
    textColor: "#333",
    style: {
      background: "#fff",
      border: "3px solid transparent",
      animation: "rainbow-border 4s linear infinite",
      boxShadow: "0 0 10px #ff0000",
    },
    buttonStyle: {
      background: "#333",
      color: "white",
      border: "none",
    },
  },
  retro_terminal: {
    name: "Retro Terminal",
    textColor: "#33ff33",
    style: {
      backgroundColor: "#1a1a1a",
      border: "2px dashed #33ff33",
      fontFamily: "monospace",
    },
    textStyle: {
      backgroundColor: "#33ff33",
    },
    buttonStyle: {
      borderColor: "#33ff33",
      color: "#33ff33",
      background: "transparent",
    },
  },
  shimmering_ocean: {
    name: "Shimmering Ocean",
    textColor: "#fff",
    style: {
      background: "linear-gradient(45deg, rgba(255,255,255,0.1) 40%, rgba(255,255,255,0.5) 50%, rgba(255,255,255,0.1) 60%), #006994",
      backgroundSize: "200% 100%",
      animation: "shimmer 2.5s infinite linear",
      border: "1px solid rgba(255,255,255,0.3)",
    },
    buttonStyle: {
      background: "rgba(0, 105, 148, 0.5)",
      color: "#fff",
    },
  },
};
