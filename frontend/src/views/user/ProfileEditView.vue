<template>
  <div class="profile-edit-container">
    <div class="container mx-auto px-4 max-w-2xl">
      <Card>
        <template #header>
          <div class="p-4 border-bottom-1 surface-border">
            <h1 class="text-3xl font-bold m-0">프로필 수정</h1>
          </div>
        </template>
        <template #content>
          <div class="flex flex-col gap-8">
            <!-- SECTION 1: 닉네임 변경 & 상태메시지 수정 -->
            <section class="flex flex-col gap-6">
              <h2 class="text-xl font-bold border-l-4 border-surface-900 dark:border-surface-0 pl-3">내 정보 수정</h2>

              <div class="flex flex-col gap-2 items-center my-8">
                <div class="relative inline-block group cursor-pointer" @click="triggerFileUpload">
                  <img v-if="previewUrl" :src="previewUrl" class="w-48 h-48 rounded-full object-cover border-4 border-surface-100 shadow-md transition-transform group-hover:scale-105" alt="Profile" />
                  <div v-else class="w-48 h-48 rounded-full bg-surface-200 flex items-center justify-center border-4 border-surface-100 shadow-md transition-transform group-hover:scale-105">
                    <i class="pi pi-user text-7xl text-gray-400"></i>
                  </div>

                  <div class="absolute inset-0 flex items-center justify-center bg-black/40 rounded-full opacity-0 group-hover:opacity-100 transition-opacity">
                    <i class="pi pi-camera text-white text-4xl"></i>
                  </div>
                </div>
                <!-- 숨겨진 파일 입력 -->
                <input type="file" ref="fileInput" accept="image/*" class="hidden" @change="handleFileChange" style="display: none" />

                <span class="text-sm text-gray-500 cursor-pointer hover:text-white underline" @click="triggerFileUpload">프로필 사진 바꾸기</span>
              </div>

              <div class="grid gap-4">
                <!-- 닉네임 -->
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium">닉네임</label>
                  <div class="flex gap-2">
                    <InputText v-model="profileForm.nickname" placeholder="닉네임 입력" class="flex-1" :class="{ 'p-invalid': nicknameState.error }" maxlength="10" @input="handleNicknameChange" />
                    <Button
                      label="중복 확인"
                      icon="pi pi-check-circle"
                      severity="secondary"
                      :loading="nicknameState.isChecking"
                      :disabled="!profileForm.nickname || profileForm.nickname === originalNickname || nicknameState.isChecked"
                      @click="handleCheckNickname"
                    />
                  </div>
                  <small v-if="nicknameState.error" class="text-red-500">{{ nicknameState.error }}</small>
                  <small v-else-if="profileForm.nickname !== originalNickname && nicknameState.isChecked" class="text-green-500">사용 가능한 닉네임입니다.</small>
                </div>

                <!-- 상태 메시지 -->
                <div class="flex flex-col gap-2">
                  <label class="text-sm font-medium">상태 메시지(20자 제한)</label>
                  <Textarea v-model="profileForm.statusMessage" placeholder="나를 표현하는 한마디" rows="3" class="w-full" maxlength="20" />
                </div>
                <!-- 저장 버튼 이동 -->
                <div class="flex justify-end pt-2">
                  <Button label="저장하기" icon="pi pi-check" :loading="savingProfile" @click="handleSaveProfile" class="w-full md:w-auto px-6" />
                </div>
              </div>
            </section>

            <Divider />

            <!-- SECTION 2: 꾸미기 설정 버튼들 (모달 트리거) -->
            <section class="flex flex-col gap-6">
              <h2 class="text-xl font-bold border-l-4 border-surface-900 dark:border-surface-0 pl-3">꾸미기</h2>

              <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <!-- 배경 테마 설정 -->
                <div class="border rounded-xl p-4 flex items-center justify-between hover:bg-surface-hover cursor-pointer transition-colors" @click="openThemeEditor">
                  <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-lg bg-surface-200 flex items-center justify-center border">
                      <i class="pi pi-palette text-xl"></i>
                    </div>
                    <div>
                      <h3 class="font-bold m-0 text-sm">프로필 배경 테마</h3>
                      <p class="text-xs text-muted m-0 mt-1">현재: {{ currentThemeName }}</p>
                    </div>
                  </div>
                  <Button icon="pi pi-chevron-right" text rounded />
                </div>

                <!-- 팝오버 설정 -->
                <div class="border rounded-xl p-4 flex items-center justify-between hover:bg-surface-hover cursor-pointer transition-colors" @click="openPopoverEditor">
                  <div class="flex items-center gap-3">
                    <div class="w-12 h-12 rounded-lg bg-surface-200 flex items-center justify-center border">
                      <i class="pi pi-comment text-xl"></i>
                    </div>
                    <div>
                      <h3 class="font-bold m-0 text-sm">팝오버 디자인</h3>
                      <p class="text-xs text-muted m-0 mt-1">현재: {{ currentPopoverName }}</p>
                    </div>
                  </div>
                  <Button icon="pi pi-chevron-right" text rounded />
                </div>
              </div>
            </section>

            <Divider />

            <!-- SECTION 3: 비밀번호/탈퇴 -->
            <!-- ... 기존 비밀번호/탈퇴 섹션 ... -->
            <section class="flex items-center justify-between p-4 rounded-lg action-card">
              <div class="flex flex-col">
                <h3 class="text-lg font-bold">비밀번호 변경</h3>
                <span class="text-sm muted-text">주기적인 비밀번호 변경으로 계정을 보호하세요.</span>
              </div>
              <Button label="변경하기" icon="pi pi-key" severity="help" outlined @click="showPasswordDialog = true" />
            </section>

            <section class="flex items-center justify-between p-4 rounded-lg danger-card mt-2">
              <div class="flex flex-col">
                <h3 class="text-lg font-bold">회원 탈퇴</h3>
                <span class="text-sm danger-muted">탈퇴 시 모든 데이터가 삭제되며 복구할 수 없습니다.</span>
              </div>
              <Button label="탈퇴하기" icon="pi pi-user-minus" severity="danger" outlined @click="showDeleteDialog = true" />
            </section>
          </div>
        </template>
      </Card>

      <!-- 비밀번호 변경 모달 & 탈퇴 모달 (기존) -->
      <Dialog v-model:visible="showPasswordDialog" modal header="비밀번호 변경" :style="{ width: '90vw', maxWidth: '400px' }">
        <div class="flex flex-col gap-4 pt-2">
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">현재 비밀번호</label>
            <Password v-model="passwordForm.currentPassword" toggleMask :feedback="false" placeholder="현재 비밀번호 입력" class="w-full" inputClass="w-full" />
          </div>
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">새 비밀번호</label>
            <Password v-model="passwordForm.newPassword" toggleMask placeholder="새 비밀번호 입력" class="w-full" inputClass="w-full">
              <template #footer>
                <Divider />
                <p class="mt-2">Suggestions</p>
                <ul class="pl-2 ml-2 mt-0" style="line-height: 1.5">
                  <li>At least one lowercase</li>
                  <li>At least one uppercase</li>
                  <li>At least one numeric</li>
                  <li>Minimum 8 characters</li>
                </ul>
              </template>
            </Password>
          </div>
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">새 비밀번호 확인</label>
            <Password v-model="passwordForm.confirmPassword" toggleMask :feedback="false" placeholder="새 비밀번호 다시 입력" class="w-full" inputClass="w-full" />
          </div>
        </div>
        <template #footer>
          <Button label="취소" icon="pi pi-times" text @click="showPasswordDialog = false" />
          <Button label="변경하기" icon="pi pi-check" :loading="changingPassword" @click="handleChangePassword" />
        </template>
      </Dialog>

      <Dialog v-model:visible="showDeleteDialog" modal header="회원 탈퇴" :style="{ width: '90vw', maxWidth: '400px' }">
        <div class="flex flex-col gap-4 pt-2">
          <div class="bg-red-50 p-3 rounded text-red-700 text-sm">
            <i class="pi pi-exclamation-triangle mr-2"></i>
            정말로 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다.
          </div>
          <div class="flex flex-col gap-2">
            <label class="text-sm font-medium">비밀번호 확인</label>
            <Password v-model="deletePassword" toggleMask :feedback="false" placeholder="비밀번호를 입력하세요" class="w-full" inputClass="w-full" />
          </div>
        </div>
        <template #footer>
          <Button label="취소" icon="pi pi-times" text @click="showDeleteDialog = false" />
          <Button label="탈퇴하기" icon="pi pi-trash" severity="danger" :loading="deletingAccount" @click="handleDeleteAccount" />
        </template>
      </Dialog>

      <!-- 커스터마이징 모달 (아바타/테마/팝오버 공용) -->
      <Dialog v-model:visible="showCustomDialog" :header="customDialogTitle" :modal="true" :style="{ width: '90vw', maxWidth: '600px' }" :draggable="false">
        <div class="flex flex-col gap-6">
          <!-- 미리보기 영역 -->
          <div class="preview-area bg-surface-ground p-6 rounded-xl border flex items-center justify-center relative overflow-hidden" :style="previewContainerStyle">
            <!-- 테마 미리보기 -->
            <div v-if="customType === 'THEME'" class="w-full overflow-hidden rounded-xl border relative bg-gray-50 dark:bg-gray-900" style="height: 300px">
              <!-- PC 화면 대응을 위한 축소된 컨테이너 -->
              <div class="origin-top-left transform scale-[0.65] w-[154%] h-[154%] p-4">
                <ProfileHeader :user="tempPreviewUser" :previewThemeId="tempSelection" class="!mb-0 shadow-lg" />
              </div>
            </div>

            <!-- 팝오버 미리보기 -->
            <div v-if="customType === 'POPOVER'" class="text-center p-4">
              <UserPopoverCard :profile="tempPreviewUser" :previewDecorationId="tempSelection">
                <template #action-button>
                  <Button icon="pi pi-user-plus" size="small" outlined class="!text-xs !px-3" />
                </template>
                <template #bottom-button>
                  <Button class="w-full mt-4 !text-sm" severity="secondary" outlined label="프로필 보기" />
                </template>
              </UserPopoverCard>
            </div>
          </div>

          <!-- 아이템 선택기 (캐러셀/그리드) -->
          <div>
            <h3 class="text-lg font-bold mb-3">보유한 아이템</h3>
            <div class="grid grid-cols-3 sm:grid-cols-4 gap-3">
              <div
                v-for="item in availableItems"
                :key="item.id || item.key"
                class="cursor-pointer border-2 rounded-lg p-2 aspect-square flex flex-col items-center justify-center gap-2 hover:bg-surface-hover transition-all relative overflow-hidden group"
                :class="{ 'border-surface-900 dark:border-surface-0 ring-2 ring-surface-900/20 dark:ring-surface-0/20': isItemSelected(item) }"
                @click="selectItem(item)"
                v-tooltip.bottom="item.description || item.name"
              >
                <div v-if="item.isDefault" class="absolute top-1 left-1 bg-gray-600/80 text-white text-[10px] px-1.5 py-0.5 rounded-full z-10 font-bold">기본</div>
                <!-- 아이템 썸네일 -->
                <div class="flex-1 w-full flex items-center justify-center overflow-hidden">
                  <img v-if="item.image" :src="resolveImageUrl(item.image)" class="w-full h-full object-contain" />
                  <div v-else class="w-full h-full rounded bg-surface-200" :style="item.style"></div>
                </div>
                <span class="text-xs truncate w-full text-center">{{ item.name }}</span>

                <div v-if="item.description" class="absolute inset-x-0 bottom-0 bg-black/70 text-white text-[10px] p-1 opacity-0 group-hover:opacity-100 transition-opacity truncate text-center z-10">
                  {{ item.description }}
                </div>

                <!-- 선택 시 체크 표시 -->
                <div v-if="isItemSelected(item)" class="absolute top-1 right-1 bg-surface-900 dark:bg-surface-0 text-surface-0 dark:text-surface-900 rounded-full w-5 h-5 flex items-center justify-center text-xs">
                  <i class="pi pi-check" style="font-size: 0.6rem"></i>
                </div>
              </div>
            </div>
          </div>
        </div>
        <template #footer>
          <Button label="취소" text @click="showCustomDialog = false" />
          <Button label="적용하기" icon="pi pi-check" @click="applyCustomization" />
        </template>
      </Dialog>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive, watch } from "vue";
import { useRouter } from "vue-router";
import { useToast } from "primevue/usetoast";
import { useAuthStore } from "@/stores/auth";
import { useCustomItemStore } from "@/stores/customItemStore"; // 스토어 가져오기
import { updateMyProfile, changePassword, deleteAccount, getMe, checkNickname } from "@/api/user";
import ProfileHeader from "@/components/user/ProfileHeader.vue";
import UserPopoverCard from "@/components/common/UserPopoverCard.vue";
import { resolveImageUrl } from "@/lib/image";
import Button from "primevue/button";
import InputText from "primevue/inputtext";
import Textarea from "primevue/textarea";
import Divider from "primevue/divider";
import Card from "primevue/card";
import Dialog from "primevue/dialog";
import Password from "primevue/password";
import FileUpload from "primevue/fileupload";

const router = useRouter();
const toast = useToast();
const authStore = useAuthStore();
const customItemStore = useCustomItemStore();

// UI 제어
const showPasswordDialog = ref(false);
const showDeleteDialog = ref(false);

// 커스터마이징 대화상자 상태
const showCustomDialog = ref(false);
const customType = ref<"THEME" | "POPOVER">("THEME");

const profileForm = ref({
  nickname: "",
  statusMessage: "",
  profileTheme: null,
  popoverDecoration: null,
});

// 모달 임시 상태
const tempSelection = ref<string | number | null>(null);
const tempFile = ref<File | null>(null);
const tempPreviewUrl = ref<string>("");

//  원래 닉네임 보관용 (변경 여부 판단)
const originalNickname = ref("");

//  닉네임 중복 확인 상태 관리
const nicknameState = ref({
  isChecked: true, // 초기값은 본인 닉네임이므로 true
  isChecking: false,
  error: "",
});

const selectedFile = ref<File | null>(null);
const previewUrl = ref("");

// 비밀번호 변경 폼
const passwordForm = ref({
  currentPassword: "",
  newPassword: "",
  confirmPassword: "",
});

// 회원탈퇴 비밀번호
const deletePassword = ref("");

const savingProfile = ref(false);
const changingPassword = ref(false);
const deletingAccount = ref(false);

const loadProfile = async () => {
  try {
    const user = await getMe();
    profileForm.value.nickname = user.nickname || "";
    profileForm.value.statusMessage = user.statusMessage || "";
    profileForm.value.profileTheme = user.profileTheme || null;
    profileForm.value.popoverDecoration = user.popoverDecoration || null;

    originalNickname.value = user.nickname || "";
    nicknameState.value.isChecked = true;
    nicknameState.value.error = "";

    // Server Image
    if (user.profileImageUrl) {
      previewUrl.value = resolveImageUrl(user.profileImageUrl);
    } else {
      previewUrl.value = "";
    }
  } catch (error: any) {
    console.error(error);
  }
};

// --- 커스터마이징 로직 ---
onMounted(async () => {
  await Promise.all([
    loadProfile(),
    customItemStore.fetchUserItems(),
    customItemStore.fetchItemDefinitions(), // 정의 로드 보장
  ]);
});

// 타입에 따른 사용 가능한 아이템 계산
const availableItems = computed(() => {
  let items = [];
  if (customType.value === "THEME") items = Object.values(customItemStore.themeItems);
  else if (customType.value === "POPOVER") items = Object.values(customItemStore.popoverItems);

  return items;
});

const customDialogTitle = computed(() => {
  if (customType.value === "THEME") return "프로필 테마 설정";
  return "팝오버 디자인 설정";
});

// 버튼 라벨용 현재 아이템 이름 가져오기 도우미
const currentThemeName = computed(() => {
  if (!profileForm.value.profileTheme) return "기본";
  // customItemStore.themeItems는 딕셔너리가 아닌 배열임
  const item = customItemStore.themeItems.find((i) => i.id == profileForm.value.profileTheme || i.key == profileForm.value.profileTheme);
  return item ? item.name : "Unknown";
});
const currentPopoverName = computed(() => {
  if (!profileForm.value.popoverDecoration) return "기본";
  // customItemStore.popoverItems는 배열임
  const item = customItemStore.popoverItems.find((i) => i.id == profileForm.value.popoverDecoration || i.key == profileForm.value.popoverDecoration);
  return item ? item.name : "Unknown";
});

// 편집기 열기
const openThemeEditor = () => {
  customType.value = "THEME";
  tempSelection.value = profileForm.value.profileTheme;
  showCustomDialog.value = true;
};

const openPopoverEditor = () => {
  customType.value = "POPOVER";
  tempSelection.value = profileForm.value.popoverDecoration;
  showCustomDialog.value = true;
};

// 모달 내 선택 처리
const selectItem = (item: any) => {
  // ID 또는 Key로 저장.
  // 정적 아이템의 경우 ID가 Key와 같을 수 있음.
  tempSelection.value = item.id || item.key;
};

const isItemSelected = (item: any) => {
  const current = tempSelection.value;
  const target = item.id || item.key;

  // 루프 이슈 디버깅 - 단순화
  // console.log('Debug Item:', item, 'Target:', target, 'Current:', current);

  // 타입(문자열 vs 숫자) 불일치 방지를 위한 느슨한 비교
  const match = current == target || String(current) === String(target);
  return match;
};

// 모달용 미리보기 데이터 계산
const tempPreviewUser = computed(() => {
  // PopoverCard는 followerCount 등이 포함된 'profile' 객체가 필요함.
  // AuthUser에 모든 필드가 없을 수 있으므로(getMe), 모의(mock) 데이터를 사용.
  return {
    ...authStore.user,
    followerCount: authStore.user?.followerCount || 10, // 미리보기용 모의 데이터
    followingCount: authStore.user?.followingCount || 5, // 미리보기용 모의 데이터
    nickname: profileForm.value.nickname,
    statusMessage: profileForm.value.statusMessage,
    profileImageUrl: tempPreviewUrl.value || authStore.user?.profileImageUrl,
    // 미리보기용 오버라이드
    profileTheme: customType.value === "THEME" ? tempSelection.value : profileForm.value.profileTheme,
    popoverDecoration: customType.value === "POPOVER" ? tempSelection.value : profileForm.value.popoverDecoration,
  };
});

const previewContainerStyle = computed(() => {
  // 테마 미리보기인 경우 컨테이너에 테마 배경 적용?
  // 아니오, 미리보기 영역은 중립 유지, 컴포넌트만 표시.
  return {};
});

// 모달 내 파일 업로드 (아바타)
const onFileCustomSelect = (event: any) => {
  const file = event.files[0];
  if (file) {
    tempFile.value = file;
    tempPreviewUrl.value = URL.createObjectURL(file);
  }
};

// 모달에서 변경 사항 적용
// 변경사항 적용 (즉시 저장)
const applyCustomization = async () => {
  try {
    const updateData: any = {};

    // 타입에 따른 데이터 준비
    // 타입에 따른 데이터 준비
    if (customType.value === "THEME") {
      profileForm.value.profileTheme = tempSelection.value;
      updateData.profileTheme = tempSelection.value;
    } else if (customType.value === "POPOVER") {
      profileForm.value.popoverDecoration = tempSelection.value;
      updateData.popoverDecoration = tempSelection.value;
    }

    // 아이템 업데이트 API 요청 (위의 파일 업로드 블록에서 처리되지 않은 경우)
    // 변경된 필드만 포함하여 업데이트 (또는 백엔드 요구사항에 따라 전체 객체 전송)
    const payload = {
      ...profileForm.value, // 기존 필드 유지
      ...updateData,
    };

    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(payload)], { type: "application/json" }));

    await updateMyProfile(formData);
    await authStore.refreshUser(); // 전역 유저 상태 갱신

    toast.add({ severity: "success", summary: "적용 완료", detail: "아이템이 적용되었습니다.", life: 3000 });
    showCustomDialog.value = false;
  } catch (error: any) {
    console.error(error);
    toast.add({ severity: "error", summary: "실패", detail: "적용에 실패했습니다.", life: 3000 });
  }
};

// --- 기존 로직 ---

// 파일 업로드 로직
const fileInput = ref<HTMLInputElement | null>(null);

const triggerFileUpload = () => {
  fileInput.value?.click();
};

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    const file = input.files[0];
    selectedFile.value = file;
    previewUrl.value = URL.createObjectURL(file);
  }
};

const handleNicknameChange = () => {
  nicknameState.value.error = "";
  if (profileForm.value.nickname === originalNickname.value) {
    nicknameState.value.isChecked = true;
  } else {
    nicknameState.value.isChecked = false;
  }
};

const handleCheckNickname = async () => {
  const nickname = profileForm.value.nickname;
  if (!nickname || nickname.length < 2) {
    nicknameState.value.error = "닉네임은 2자 이상이어야 합니다.";
    return;
  }

  // 금칙어 검사
  if (nickname.includes("admin") || nickname.includes("관리자") || nickname.includes("운영자")) {
    nicknameState.value.error = "사용할 수 없는 닉네임입니다.";
    return;
  }

  nicknameState.value.isChecking = true;
  nicknameState.value.error = "";

  try {
    await checkNickname(nickname);
    nicknameState.value.isChecked = true;
    toast.add({ severity: "success", summary: "확인 완료", detail: "사용 가능한 닉네임입니다.", life: 3000 });
  } catch (error: any) {
    nicknameState.value.isChecked = false;
    const msg = error.response?.data?.message || "이미 사용 중인 닉네임입니다.";
    nicknameState.value.error = msg;
    toast.add({ severity: "error", summary: "중복", detail: msg, life: 3000 });
  } finally {
    nicknameState.value.isChecking = false;
  }
};

const handleSaveProfile = async () => {
  if (!nicknameState.value.isChecked) {
    toast.add({ severity: "warn", summary: "확인 필요", detail: "닉네임 중복 확인을 해주세요.", life: 3000 });
    return;
  }

  if (nicknameState.value.error) {
    toast.add({ severity: "warn", summary: "확인 필요", detail: "닉네임을 확인해주세요.", life: 3000 });
    return;
  }

  savingProfile.value = true;
  try {
    const formData = new FormData();
    formData.append("data", new Blob([JSON.stringify(profileForm.value)], { type: "application/json" }));

    if (selectedFile.value) {
      formData.append("file", selectedFile.value);
    }

    await updateMyProfile(formData);
    await authStore.refreshUser();

    originalNickname.value = profileForm.value.nickname;
    nicknameState.value.isChecked = true;

    toast.add({ severity: "success", summary: "성공", detail: "프로필이 업데이트 되었습니다.", life: 3000 });
    selectedFile.value = null;
  } catch (error: any) {
    toast.add({ severity: "error", summary: "실패", detail: "업데이트에 실패했습니다.", life: 3000 });
  } finally {
    savingProfile.value = false;
  }
};

const handleChangePassword = async () => {
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    toast.add({ severity: "warn", summary: "주의", detail: "비밀번호가 일치하지 않습니다.", life: 3000 });
    return;
  }
  if (passwordForm.value.newPassword.length < 8) {
    toast.add({ severity: "warn", summary: "주의", detail: "비밀번호는 최소 8자 이상이어야 합니다.", life: 3000 });
    return;
  }

  changingPassword.value = true;
  try {
    await changePassword(passwordForm.value.currentPassword, passwordForm.value.newPassword);
    passwordForm.value = { currentPassword: "", newPassword: "", confirmPassword: "" };
    showPasswordDialog.value = false;
    toast.add({ severity: "success", summary: "성공", detail: "비밀번호가 변경되었습니다.", life: 3000 });
  } catch (error: any) {
    toast.add({ severity: "error", summary: "오류", detail: error.response?.data?.message || "비밀번호 변경에 실패했습니다.", life: 3000 });
  } finally {
    changingPassword.value = false;
  }
};

const handleDeleteAccount = async () => {
  if (!deletePassword.value) {
    toast.add({ severity: "warn", summary: "확인 필요", detail: "비밀번호를 입력해주세요.", life: 3000 });
    return;
  }

  deletingAccount.value = true;
  try {
    await deleteAccount(deletePassword.value);
    toast.add({ severity: "success", summary: "성공", detail: "계정이 삭제되었습니다.", life: 3000 });
    authStore.logoutUser();
    showDeleteDialog.value = false;
    router.push("/");
  } catch (error: any) {
    toast.add({ severity: "error", summary: "오류", detail: error.response?.data?.message || "계정 삭제에 실패했습니다.", life: 3000 });
  } finally {
    deletingAccount.value = false;
  }
};

// 템플릿용 최상위 값
const previewUser = computed(() => {
  return {
    ...authStore.user,
    ...profileForm.value,
    profileImageUrl: previewUrl.value || authStore.user?.profileImageUrl,
  };
});
</script>

<style scoped>
.profile-edit-container {
  min-height: calc(100vh - 100px);
  padding-bottom: 3rem;
}

:deep(.custom-upload .p-button-label) {
  display: none;
}

/* 모달 전용 */
.preview-area {
  min-height: 200px;
  background-color: var(--surface-100);
}

/* ... 기존 스타일 ... */
:deep(.custom-upload input[type="file"]) {
  opacity: 0 !important;
  width: 0 !important;
  height: 0 !important;
  position: absolute !important;
  z-index: -1 !important;
  overflow: hidden !important;
}

:deep(.p-fileupload-choose) {
  border-radius: 50%;
  padding: 0 !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
}

:deep(.p-fileupload) span {
  display: none !important;
}

.scrollbar-hide {
  -ms-overflow-style: none; /* IE, Edge */
  scrollbar-width: none; /* Firefox */
}
.scrollbar-hide::-webkit-scrollbar {
  display: none; /* Chrome, Safari, Opera */
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.action-card {
  background: var(--bg-surface);
  border: 1px solid var(--border);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.05); /* 그림자 투명도 조정 */
}

.danger-card {
  background: rgba(255, 99, 71, 0.08); /* 기능적 빨간색 유지 */
  border: 1px solid rgba(255, 99, 71, 0.26);
  box-shadow: 0 12px 32px rgba(255, 99, 71, 0.15);
}

.muted-text {
  color: var(--text-sub);
}

.danger-muted {
  color: #fca5a5;
}

:global([data-theme="dark"] .action-card) {
  background: var(--bg-surface);
  border-color: var(--border);
  box-shadow: 0 16px 40px rgba(0, 0, 0, 0.35);
}

:global([data-theme="dark"] .danger-card) {
  background: rgba(239, 68, 68, 0.14);
  border-color: rgba(239, 68, 68, 0.32);
  box-shadow: 0 18px 44px rgba(239, 68, 68, 0.2);
}

:global([data-theme="dark"] .muted-text) {
  color: var(--text-sub);
}
</style>
