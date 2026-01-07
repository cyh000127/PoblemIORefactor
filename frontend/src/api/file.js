import apiClient from './axios'

// 공용 파일 업로드 API (이미지 등)
// category: 'thumbnail' | 'question' 등 서버에서 허용된 하위 폴더 이름
export const uploadFile = async (file, category) => {
  const formData = new FormData()
  formData.append('file', file)

  const response = await apiClient.post('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    params: category ? { category } : undefined,
  })

  return response.data.data // { url, filename }
}
