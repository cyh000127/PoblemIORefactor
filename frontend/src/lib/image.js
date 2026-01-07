const apiBaseRaw = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api')
const apiBaseUrl = apiBaseRaw.endsWith('/api') ? apiBaseRaw : `${apiBaseRaw}`
const staticBaseUrl = apiBaseUrl.replace(/\/api$/, '')
// const s3BaseUrl = (import.meta.env.VITE_S3_BASE_URL).replace(/\/+$/, '') // Removed S3 dependency

export const resolveImageUrl = (url) => {
  if (!url) return '' // Return empty string instead of undefined/null to prevent UI errors
  if (typeof url !== 'string') return ''

  if (url.startsWith('http') || url.startsWith('blob:') || url.startsWith('data:')) return url

  // 로컬 파일 서빙 경로 확인
  // 백엔드 WebMvcConfig에서 /uploads/** -> C:/public/upload/** 로 매핑함

  // 이미 /uploads/로 시작하면 그대로 반환
  if (url.startsWith('/uploads/')) {
    return url;
  }

  // public/으로 시작하면 /uploads/ 붙여서 반환
  if (url.startsWith('public/') || url.startsWith('/public/')) {
    const cleanPath = url.startsWith('/') ? url.substring(1) : url;
    return `/uploads/${cleanPath}`;
  }

  // 그 외의 경우 staticBaseUrl 사용 (기존 로직 유지)
  const prefix = url.startsWith('/') ? '' : '/'
  return `${staticBaseUrl}${prefix}${url}`
}

export const resolveQuizImages = (quiz) => {
  if (!quiz) return quiz
  const resolved = { ...quiz }
  resolved.thumbnailUrl = resolveImageUrl(quiz.thumbnailUrl)

  if (quiz.questions) {
    resolved.questions = quiz.questions.map((q) => ({
      ...q,
      imageUrl: resolveImageUrl(q.imageUrl),
    }))
  }
  return resolved
}

// 레거시 로컬 업로드 URL을 S3 경로로 변환
const remapLegacyUploadUrl = (url) => {
  const lower = url.toLowerCase()
  const marker = '/uploads/'

  // host가 포함된 경우
  if (lower.startsWith('http://localhost:8080/uploads/') || lower.startsWith('https://localhost:8080/uploads/')) {
    const idx = lower.indexOf(marker)
    const path = url.substring(idx + marker.length)
    return buildS3FromUploads(path)
  }

  // 절대 경로만 온 경우
  if (lower.startsWith('/uploads/')) {
    const path = url.substring(marker.length)
    return buildS3FromUploads(path)
  }

  return null
}

// 로컬 업로드 경로로 변환
// uploads/* → /uploads/public/upload/* (구조에 따라 조정 필요)
// 현재 백엔드 로직: /uploads/** -> C:/public/upload/**
// DB에 저장된 키가 "public/upload/questions/..." 형태라면 -> /uploads/public/upload/questions/...
const buildS3FromUploads = (path) => {
  const clean = path.startsWith('/') ? path.substring(1) : path

  // 간단하게 /uploads/ prefix만 붙여서 반환 (백엔드 리소스 핸들러가 처리)
  return `/uploads/public/upload/${clean}`
}
