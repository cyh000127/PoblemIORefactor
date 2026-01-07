// [수정] 로컬 이미지 경로 사용
// const S3_PUBLIC_BASE = "https://s3.ap-northeast-2.amazonaws.com/s3-problemio";

const withS3Public = (path) => {
  if (!path) return path;

  // 로컬 업로드 경로로 변환
  const clean = path.startsWith("/") ? path.slice(1) : path;
  return `/uploads/${clean}`;
};

export const PROFILE_THEMES = {
  default: {
    name: "Default",
    // 특정 스타일 필요 없음, 전역 라이트/다크 모드에 적응
  },
  cybercity: {
    name: "Cybercity",
    image: withS3Public("public/theme/cybercity.jpg"),
    textColor: "#ffffff",
    style: {
      textShadow: "0 0 5px #00eaff, 0 0 10px #00eaff",
      WebkitTextStroke: "1px #00eaff",
      border: "2px solid #00eaff",
      animation: "neon-pulse 2s infinite ease-in-out, border-flow 4s infinite linear",
    },
  },

  aurora_night: {
    name: "Aurora Night",
    textColor: "#e0f7ff",
    style: {
      background: "linear-gradient(270deg, #1c1c3c, #3b2f63, #355c7d, #1c1c3c)",
      backgroundSize: "400% 400%",
      animation: "gradient-move 10s ease infinite",
    },
  },
  sunset_vibes: {
    name: "Sunset Vibes",
    textColor: "#5d3a3a",
    style: {
      background: "linear-gradient(45deg, #ff9a9e 0%, #fad0c4 99%, #fad0c4 100%)",
      backgroundSize: "200% 200%",
      animation: "gradient-move 5s ease infinite alternate",
    },
  },
  cyber_punk: {
    name: "Cyber Punk",
    textColor: "#00ff41",
    style: {
      backgroundColor: "#050505",
      backgroundImage:
        "linear-gradient(0deg, transparent 24%, rgba(0, 255, 0, .05) 25%, rgba(0, 255, 0, .05) 26%, transparent 27%, transparent 74%, rgba(0, 255, 0, .05) 75%, rgba(0, 255, 0, .05) 76%, transparent 77%, transparent), linear-gradient(90deg, transparent 24%, rgba(0, 255, 0, .05) 25%, rgba(0, 255, 0, .05) 26%, transparent 27%, transparent 74%, rgba(0, 255, 0, .05) 75%, rgba(0, 255, 0, .05) 76%, transparent 77%, transparent)",
      backgroundSize: "50px 50px",
      boxShadow: "inset 0 0 50px #000",
    },
  },
  golden_luxury: {
    name: "Golden Luxury",
    textColor: "#5a3e1a",
    style: {
      background: "linear-gradient(120deg, #f6d365 0%, #fda085 100%)",
      position: "relative",
      animation: "shimmer 3s infinite linear", // 고급스러운 느낌을 위해 쉬머 효과 복구
      backgroundSize: "200% auto",
    },
  },

  matrix_code: {
    name: "Matrix Code",
    image: withS3Public("public/theme/cyberpunk.jpg"),
    textColor: "#00FF00",
    style: {
      backgroundColor: "#000",
      fontFamily: "monospace",
      border: "2px solid #00FF00",
      boxShadow: "0 0 10px #00FF00",
    },
  },

  minimalist_float: {
    name: "Minimalist Float",
    textColor: "#111827",
    style: {
      backgroundColor: "#fff",
      backgroundImage: "radial-gradient(#e5e7eb 2px, transparent 2px)",
      backgroundSize: "20px 20px",
      animation: "floating 3s ease-in-out infinite",
    },
  },
};
