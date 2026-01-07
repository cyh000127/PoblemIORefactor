<template>
  <div class="relative inline-block" :style="containerStyle">
    <Avatar
      :image="image"
      :label="label"
      :icon="icon"
      shape="circle" 
      :class="avatarClass"
      :style="avatarStyle"
    />
    <img 
      v-if="decorationUrl" 
      :src="decorationUrl" 
      class="absolute top-0 left-0 w-full h-full object-contain pointer-events-none z-10"
      alt="decoration"
    />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { resolveImageUrl } from '@/lib/image'
import { AVATAR_DECORATIONS } from '@/constants/avatarConfig'

const props = defineProps({
  user: {
    type: Object,
    default: null,
  },
  // PrimeVue Avatar 크기 prop (normal, large, xlarge - 또는 명시적 스타일링)
  size: {
    type: String,
    default: 'xlarge',
  },
})

const authStore = useAuthStore()
const targetUser = computed(() => props.user || authStore.user)

const image = computed(() => {
  const u = targetUser.value
  if (!u?.profileImageUrl) return null
  return resolveImageUrl(u.profileImageUrl)
})

const label = computed(() => {
  const u = targetUser.value
  if (!u?.profileImageUrl && u?.nickname) {
    return u.nickname.charAt(0).toUpperCase()
  }
  return ''
})

const icon = computed(() => {
  const u = targetUser.value
  if (!u?.profileImageUrl && !u?.nickname) {
    return 'pi pi-user'
  }
  return ''
})

const decorationUrl = computed(() => {
  const u = targetUser.value
  if (!u?.avatarDecoration) return null
  const deco = AVATAR_DECORATIONS[u.avatarDecoration]
  return deco ? resolveImageUrl(deco.image) : null
})

// 크기 매핑: PrimeVue Avatar 크기 클래스는 커스텀 래퍼 크기 조정에 충분하지 않을 수 있음
// 데코레이션을 포함하기 위해 'size' prop을 래퍼의 명시적 픽셀 크기로 매핑함
const sizeMap = {
  small: '1.5rem',
  normal: '2rem', // 32px
  medium: '2.5rem',
  large: '3rem',  // 48px
  xlarge: '4rem', // 64px
}

const containerStyle = computed(() => {
  const s = sizeMap[props.size] || '4rem' // 알 수 없는 경우 기본값 xlarge
  return { width: s, height: s }
})

const avatarClass = computed(() => {
  const classes = []
  if (props.size) classes.push(`p-avatar-${props.size}`)
  return classes
})

const avatarStyle = computed(() => {
  return { 
    width: '100%', 
    height: '100%',
    // 테마에서 상속된 텍스트 스타일 초기화 (아바타 텍스트에 네온 효과 방지)
    textShadow: 'none',
    WebkitTextStroke: '0',
    color: '#374151', // 아바타 내부 가독성을 위해 어두운 텍스트 강제 (gray-700)
    objectFit: 'cover', // 이미지 늘어짐 방지
  }
})
</script>

<style scoped>
:deep(.p-avatar img) {
    object-fit: cover;
    width: 100%;
    height: 100%;
}
</style>
