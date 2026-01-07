<template>
  <div class="admin-container">
    <div class="container mx-auto px-4">
      <h1 class="page-title">관리자 대시보드</h1>

      <!-- 탭 -->
      <div class="tab-navbar">
        <button :class="['tab-button', { active: activeTab === 'quizzes' }]" @click="activeTab = 'quizzes'">퀴즈 관리</button>
        <button :class="['tab-button', { active: activeTab === 'challenges' }]" @click="activeTab = 'challenges'">챌린지 관리</button>
        <button :class="['tab-button', { active: activeTab === 'custom-items' }]" @click="activeTab = 'custom-items'">커스텀 관리</button>
      </div>

      <!-- 퀴즈 관리 탭 -->
      <div v-show="activeTab === 'quizzes'" class="tab-content">
        <!-- 검색 바 -->
        <div class="search-bar-wide w-full mb-6 flex justify-center">
          <span class="p-input-icon-right search-input-wrapper" style="max-width: 600px; width: 100%">
            <InputText v-model="searchKeyword" placeholder="퀴즈 검색..." class="search-input w-full" @keyup.enter="loadQuizzes" />
            <i class="pi pi-search search-icon" @click="loadQuizzes"></i>
          </span>
        </div>

        <div v-if="loading" class="text-center py-8">
          <i class="pi pi-spin pi-spinner text-4xl" style="color: var(--text-main)"></i>
        </div>

        <div v-else-if="quizzes.length === 0" class="text-center py-8 text-secondary">
          <p class="text-xl">퀴즈가 없습니다.</p>
        </div>

        <div v-else class="quiz-grid-container mb-6">
          <div v-for="quiz in quizzes" :key="quiz.id" class="quiz-card">
            <div class="quiz-thumbnail">
              <img :src="resolveImageUrl(quiz.thumbnailUrl) || '/placeholder.svg'" :alt="quiz.title" class="quiz-thumbnail-img" />
              <div v-if="quiz.hidden" class="hidden-badge">숨김</div>
            </div>
            <div class="quiz-body">
              <div class="quiz-meta">
                <h3 class="quiz-title">{{ quiz.title }}</h3>
                <div class="quiz-stat">
                  <span>{{ quiz.author?.nickname || "알 수 없음" }}</span>
                </div>
              </div>
              <div class="quiz-actions mt-auto">
                <Button
                  :label="quiz.hidden ? '보이기' : '숨기기'"
                  :icon="quiz.hidden ? 'pi pi-eye' : 'pi pi-eye-slash'"
                  :severity="quiz.hidden ? 'success' : 'warn'"
                  size="small"
                  outlined
                  class="w-full text-xs"
                  @click="handleToggleHide(quiz)"
                />
              </div>
            </div>
          </div>
        </div>

        <Paginator
          v-if="quizzes.length > 0"
          :first="(page - 1) * size"
          :rows="size"
          :totalRecords="totalRecords"
          @page="onPageChange"
          template="PrevPageLink PageLinks NextPageLink"
          class="custom-paginator"
        />
      </div>

      <!-- 챌린지 관리 탭 -->
      <div v-show="activeTab === 'challenges'" class="tab-content">
        <div class="challenge-form-card">
          <h2 class="form-title">새 챌린지 생성</h2>

          <div class="field mb-4">
            <label class="field-label">챌린지 유형</label>
            <div class="flex gap-4">
              <div class="flex align-items-center">
                <RadioButton v-model="challengeForm.challengeType" inputId="survival" name="type" value="SURVIVAL" />
                <label for="survival" class="ml-2 field-label-text">서바이벌</label>
              </div>
              <div class="flex align-items-center">
                <RadioButton v-model="challengeForm.challengeType" inputId="timeattack" name="type" value="TIME_ATTACK" />
                <label for="timeattack" class="ml-2 field-label-text">타임어택</label>
              </div>
            </div>
          </div>

          <div class="field mb-4">
            <label class="field-label">제목</label>
            <InputText v-model="challengeForm.title" class="w-full search-input" placeholder="챌린지 제목을 입력하세요" />
          </div>

          <div class="field mb-4">
            <label class="field-label">설명</label>
            <Textarea v-model="challengeForm.description" rows="3" class="w-full search-input" placeholder="챌린지 설명을 입력하세요" />
          </div>

          <!-- 퀴즈 선택기 -->
          <div class="field mb-4">
            <label class="field-label">대상 퀴즈</label>
            <div class="flex gap-2">
              <div
                class="flex-1 p-3 border rounded-lg bg-surface-card text-surface-900 border-surface-border flex items-center justify-between"
                style="background: var(--color-background-soft); border-color: var(--color-border)"
              >
                <span v-if="selectedQuiz" class="font-medium" style="color: var(--color-heading)">{{ selectedQuiz.title }} (ID: {{ selectedQuiz.id }})</span>
                <span v-else class="text-secondary">선택된 퀴즈 없음</span>
                <Button v-if="selectedQuiz" icon="pi pi-times" text rounded size="small" @click="clearSelectedQuiz" />
              </div>
              <Button label="퀴즈 선택" icon="pi pi-search" @click="openQuizPicker" outlined />
            </div>
            <input type="hidden" :value="challengeForm.targetQuizId" />
          </div>

          <div v-if="challengeForm.challengeType === 'TIME_ATTACK'" class="field mb-4">
            <label class="field-label">제한 시간 (초)</label>
            <InputNumber v-model="challengeForm.timeLimit" class="w-full" placeholder="예: 60" />
          </div>

          <div class="grid grid-cols-2 gap-4 mb-4">
            <div class="field">
              <label class="field-label">시작 일시</label>
              <DatePicker v-model="challengeForm.startAt" showTime hourFormat="24" class="w-full" showIcon />
            </div>
            <div class="field">
              <label class="field-label">종료 일시</label>
              <DatePicker v-model="challengeForm.endAt" showTime hourFormat="24" class="w-full" showIcon />
            </div>
          </div>

          <Button label="챌린지 생성" class="w-full" icon="pi pi-check" :loading="submitting" @click="submitChallenge" />
        </div>
      </div>

      <!-- 커스텀 아이템 탭 -->
      <div v-show="activeTab === 'custom-items'" class="tab-content">
        <div class="challenge-form-card mb-6">
          <h2 class="form-title">커스텀 아이템 생성</h2>
          <div class="grid grid-cols-1 gap-6">
            <!-- 1. 메타데이터 필드 (최상단) -->
            <div class="grid grid-cols-1 gap-4">
              <div class="field">
                <label class="field-label">아이템 유형</label>
                <Select v-model="newItem.itemType" :options="['THEME', 'POPOVER']" placeholder="유형 선택" class="w-full p-dropdown" />
              </div>
              <div class="field">
                <label class="field-label">이름</label>
                <InputText v-model="newItem.name" class="w-full" placeholder="테마 이름" />
              </div>
              <div class="field">
                <label class="field-label">설명</label>
                <InputText v-model="newItem.description" class="w-full" placeholder="테마 설명" />
              </div>
            </div>

            <!-- 2. 이미지 업로드 (상단) -->
            <div class="bg-surface-card border rounded-xl p-4 flex flex-col gap-4 shadow-sm border-surface-border">
              <div class="flex justify-between items-center">
                <h3 class="font-bold text-sm text-secondary flex items-center gap-2">
                  <i class="pi pi-image"></i>
                  에셋 업로드 (배경 이미지)
                </h3>
                <Button v-if="uploadedImageUrl" label="이미지 삭제" icon="pi pi-trash" size="small" severity="danger" text @click="removeImage" />
              </div>
              <div
                class="border-2 border-dashed border-surface-border rounded-xl p-4 flex items-center gap-4 cursor-pointer hover:bg-surface-hover transition-colors group bg-surface-50"
                @click="triggerFileUpload"
              >
                <div class="w-10 h-10 rounded-full bg-surface-200 flex items-center justify-center group-hover:bg-surface-300 transition-colors flex-shrink-0">
                  <i v-if="!uploadedImageUrl && !localPreviewUrl" class="pi pi-image text-lg text-secondary group-hover:text-surface-900 dark:group-hover:text-surface-0"></i>
                  <img v-else :src="localPreviewUrl || resolveImageUrl(uploadedImageUrl)" class="w-full h-full object-cover rounded-full border border-surface-border" />
                </div>
                <div class="flex-1 min-w-0">
                  <p class="font-bold text-sm m-0 text-heading truncate">
                    {{ uploadedImageUrl ? "이미지 변경" : "이미지 업로드" }}
                  </p>
                  <p class="text-xs text-secondary m-0">업로드한 이미지는 미리보기와 실제 아이템에 자동 적용됩니다.</p>
                </div>
                <i v-if="uploadedImageUrl" class="pi pi-check-circle text-surface-900 dark:text-surface-0"></i>
              </div>
              <input type="file" ref="fileInput" @change="onFileSelect" accept="image/*" class="hidden" />
            </div>

            <!-- 3. 미리보기 섹션 (중간) -->
            <div class="flex flex-col gap-2">
              <div class="flex justify-between items-center px-1">
                <label class="field-label m-0">라이브 미리보기</label>
                <Button label="미리보기 갱신" icon="pi pi-refresh" size="small" outlined @click="applyPreview" />
              </div>

              <div class="preview-area border border-surface-border p-6 rounded-xl flex items-center justify-center relative overflow-hidden min-h-[400px]">
                <!-- 라벨 -->
                <div class="absolute top-4 left-4 z-10 bg-black/50 text-white px-3 py-1 rounded-full text-xs backdrop-blur-md">Live Preview</div>

                <!-- 테마 미리보기: 축소된 컨테이너 -->
                <div v-if="newItem.itemType === 'THEME'" class="w-full overflow-hidden rounded-xl border relative shadow-sm" style="height: 300px">
                  <div class="origin-top-left transform scale-[0.65] w-[154%] h-[154%] p-4 flex items-center justify-center">
                    <ProfileHeader
                      :user="{
                        nickname: 'Preview User',
                        statusMessage: newItem.name ? newItem.name + ' Theme Preview' : 'Your Custom Theme Preview',
                        profileTheme: 'preview',
                        followerCount: 1234,
                        followingCount: 567,
                        quizCount: 89,
                      }"
                      :previewConfig="currentPreviewConfig"
                      class="!mb-0 shadow-lg w-full max-w-3xl"
                    />
                  </div>
                </div>

                <!-- 팝오버 미리보기 -->
                <div v-if="newItem.itemType === 'POPOVER'" class="flex items-center justify-center h-full w-full overflow-hidden p-6">
                  <div class="transform scale-110 origin-center transition-transform hover:scale-110">
                    <UserPopoverCard
                      :profile="{ nickname: 'Preview User', statusMessage: 'Custom Popover Preview', followerCount: 100, followingCount: 50 }"
                      :previewConfig="currentPreviewConfig"
                      class="shadow-2xl"
                    >
                      <template #action-button>
                        <Button icon="pi pi-user-plus" size="small" outlined class="!text-xs !px-3" />
                      </template>
                      <template #bottom-button>
                        <Button class="w-full mt-4 !text-sm" severity="secondary" outlined label="프로필 보기" />
                      </template>
                    </UserPopoverCard>
                  </div>
                </div>
              </div>
            </div>

            <!-- 4. 스타일 설정 (하단) -->
            <div class="flex flex-col gap-4 mb-4">
              <!-- 스타일 빌더 -->
              <div class="flex flex-col gap-2 mt-2">
                <div class="flex justify-between items-center px-1">
                  <label class="field-label m-0">스타일 설정</label>
                </div>

                <div class="p-5 border rounded-xl bg-surface-card flex flex-col gap-6 shadow-sm" style="background: var(--color-background-soft); border-color: var(--color-border)">
                  <!-- 배경 그룹 -->
                  <div class="pb-4 border-b border-dashed border-surface-border" :class="{ 'opacity-40 pointer-events-none grayscale': uploadedImageUrl || localPreviewUrl }">
                    <div class="flex justify-between items-center mb-3">
                      <span class="text-sm font-bold opacity-80 flex items-center gap-2">
                        <i class="pi pi-palette"></i>
                        배경 스타일
                        <span v-if="uploadedImageUrl || localPreviewUrl" class="text-xs font-normal text-red-400 ml-2">(이미지 사용 중 비활성화)</span>
                      </span>
                      <div class="flex items-center gap-2 text-xs bg-surface-ground p-1 rounded-lg">
                        <span :class="!builderForm.useGradient ? 'font-bold text-surface-900 dark:text-surface-0' : ''">단색</span>
                        <ToggleSwitch v-model="builderForm.useGradient" class="scale-75" />
                        <span :class="builderForm.useGradient ? 'font-bold text-surface-900 dark:text-surface-0' : ''">그라데이션</span>
                      </div>
                    </div>

                    <div v-if="!builderForm.useGradient" class="flex items-center gap-3">
                      <div class="p-1 border rounded-full"><ColorPicker v-model="builderForm.bgColor" /></div>
                      <span class="text-xs font-mono opacity-70 bg-surface-ground px-2 py-1 rounded">#{{ builderForm.bgColor }}</span>
                    </div>
                    <div v-else class="flex items-center gap-2">
                      <div class="p-1 border rounded-full"><ColorPicker v-model="builderForm.gradientStart" /></div>
                      <i class="pi pi-arrow-right text-xs opacity-50"></i>
                      <div class="p-1 border rounded-full"><ColorPicker v-model="builderForm.gradientEnd" /></div>
                    </div>
                  </div>

                  <!-- 색상 & 테두리 그룹 -->
                  <div class="grid grid-cols-2 gap-4 pb-4 border-b border-dashed border-surface-border">
                    <div>
                      <label class="block text-xs font-bold mb-2 opacity-80">텍스트 색상</label>
                      <div class="flex items-center gap-2">
                        <div class="p-1 border rounded-full"><ColorPicker v-model="builderForm.textColor" /></div>
                        <span class="text-xs font-mono opacity-70">#{{ builderForm.textColor }}</span>
                      </div>
                    </div>
                    <div>
                      <label class="block text-xs font-bold mb-2 opacity-80">테두리 색상</label>
                      <div class="flex items-center gap-2">
                        <div class="p-1 border rounded-full"><ColorPicker v-model="builderForm.borderColor" /></div>
                        <span class="text-xs font-mono opacity-70">#{{ builderForm.borderColor }}</span>
                      </div>
                    </div>
                    <div class="col-span-2 mt-2">
                      <label class="block text-xs font-bold mb-2 opacity-80 flex justify-between">
                        <span>테두리 두께</span>
                        <span class="text-surface-900 dark:text-surface-0">{{ builderForm.borderWidth }}px</span>
                      </label>
                      <Slider v-model="builderForm.borderWidth" :min="0" :max="10" />
                    </div>
                  </div>

                  <!-- 애니메이션 그룹 -->
                  <div>
                    <label class="block text-sm font-bold mb-3 opacity-80 flex items-center gap-2">
                      <i class="pi pi-bolt"></i>
                      애니메이션 효과
                    </label>
                    <MultiSelect
                      v-model="builderForm.animationNames"
                      :options="animationOptions"
                      optionLabel="label"
                      optionValue="value"
                      placeholder="효과 선택 (다중 선택 가능)..."
                      class="w-full mb-4"
                      display="chip"
                    />

                    <div v-if="builderForm.animationNames && builderForm.animationNames.length > 0" class="bg-surface-ground p-3 rounded-lg">
                      <div class="flex justify-between text-xs mb-2 opacity-70">
                        <span>빠르게 (0.5s)</span>
                        <span>느리게 (5s)</span>
                      </div>
                      <Slider v-model="builderForm.animationDuration" :min="0.5" :max="5" :step="0.1" />
                      <div class="text-center text-xs mt-2 font-mono">{{ builderForm.animationDuration }}s</div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="field col-span-2" style="grid-column: span 2">
                <div
                  class="flex items-center justify-between p-4 rounded-xl border-2 cursor-pointer transition-all duration-200 hover:bg-surface-50"
                  :class="newItem.isDefault ? 'border-surface-900 dark:border-surface-0 bg-surface-100 dark:bg-surface-800' : 'border-surface-200'"
                  @click="newItem.isDefault = !newItem.isDefault"
                >
                  <div class="flex items-center gap-3">
                    <div
                      class="w-10 h-10 rounded-full flex items-center justify-center transition-colors"
                      :style="newItem.isDefault ? 'background-color: var(--text-main); color: var(--bg-surface);' : 'background-color: var(--surface-200); color: var(--text-color-secondary);'"
                    >
                      <i class="pi" :class="newItem.isDefault ? 'pi-check' : 'pi-gift'"></i>
                    </div>
                    <div>
                      <label class="font-bold text-sm cursor-pointer select-none block" style="color: var(--color-heading)">기본 지급 아이템</label>
                      <span class="text-xs text-secondary block">
                        {{ newItem.isDefault ? "모든 신규 사용자에게 자동으로 지급됩니다." : "사용자가 획득해야 하는 아이템입니다." }}
                      </span>
                    </div>
                  </div>
                  <Checkbox v-model="newItem.isDefault" :binary="true" inputId="isDefault" class="pointer-events-none" />
                </div>
              </div>
            </div>
          </div>
          <div class="flex gap-2 mt-8">
            <Button v-if="isEditMode" label="취소" icon="pi pi-times" severity="secondary" @click="cancelEdit" class="flex-1" />
            <Button
              :label="isEditMode ? '아이템 수정' : '아이템 생성'"
              :icon="isEditMode ? 'pi pi-save' : 'pi pi-plus'"
              @click="isEditMode ? updateItem() : createItem()"
              :loading="creatingItem"
              class="flex-1"
            />
          </div>
        </div>

        <h2 class="text-xl font-bold mb-4" style="color: var(--color-heading)">아이템 목록</h2>
        <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          <div
            v-for="item in customItems"
            :key="item.id"
            class="p-4 border rounded-lg bg-surface-card relative group"
            style="background: var(--color-background-soft); border-color: var(--color-border)"
          >
            <div class="font-bold text-lg" style="color: var(--color-heading)">
              {{ item.name }}
            </div>
            <div v-if="item.isDefault" class="default-item-badge absolute top-2 right-2 bg-surface-900 dark:bg-surface-0 text-surface-0 dark:text-surface-900 text-xs px-2 py-1 rounded shadow-sm font-bold z-10">Default</div>
            <div class="text-sm mb-2" style="color: var(--text-color-secondary)">{{ item.itemType }}</div>
            <!-- JSON 미리보기 (관리자 디버그용). -->
            <div class="text-xs bg-black/5 p-2 rounded overflow-hidden h-16 mb-2 opacity-50 font-mono">
              {{ item.config ? item.config.substring(0, 100) + "..." : "{}" }}
            </div>
            <div class="flex flex-col gap-2 mt-2">
              <div class="flex justify-between gap-2">
                <Button label="유저 할당" size="small" outlined icon="pi pi-user-plus" @click="openAssignDialog(item)" class="flex-1 p-button-sm text-xs" />
                <Button label="유저 관리" size="small" outlined icon="pi pi-users" @click="openUserDialog(item)" class="flex-1 p-button-sm text-xs" />
              </div>

              <div class="flex justify-end gap-1">
                <Button icon="pi pi-pencil" size="small" text severity="info" @click="startEdit(item)" />
                <Button icon="pi pi-trash" size="small" text severity="danger" @click="confirmDelete(item)" />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 유저 관리 다이얼로그 -->
    <Dialog v-model:visible="showUserDialog" header="할당된 유저 관리" :style="{ width: '40rem' }" :modal="true">
      <div class="p-4">
        <div v-if="loadingUsers" class="text-center">
          <i class="pi pi-spin pi-spinner text-2xl"></i>
        </div>
        <div v-else-if="assignedUsers.length === 0" class="text-center text-secondary py-4">할당된 유저가 없습니다.</div>
        <div v-else class="flex flex-col gap-2">
          <div v-for="user in assignedUsers" :key="user.id" class="flex justify-between items-center p-3 border rounded bg-gray-50">
            <div class="flex items-center gap-3">
              <img :src="resolveImageUrl(user.profileImageUrl) || '/placeholder.svg'" class="w-8 h-8 rounded-full object-cover" />
              <div>
                <div class="font-bold">{{ user.nickname }}</div>
                <div class="text-xs text-gray-500">{{ user.email }}</div>
              </div>
            </div>
            <Button label="권한 취소" severity="danger" size="small" outlined @click="removeUser(user.id)" />
          </div>
        </div>
      </div>
    </Dialog>

    <!-- 유저 할당 다이얼로그 -->
    <Dialog v-model:visible="showAssignDialog" header="유저에게 아이템 할당" :style="{ width: '30rem' }" :modal="true">
      <div class="p-4">
        <div class="mb-4">
          <p class="text-color">
            대상 아이템:
            <b>{{ selectedItemToAssign?.name }}</b>
          </p>
        </div>
        <div class="field mb-4">
          <label class="field-label">유저 ID (임시)</label>
          <InputNumber v-model="assignUserId" class="w-full" placeholder="User ID" />
        </div>
        <Button label="할당하기" icon="pi pi-check" class="w-full" @click="assignItem" :loading="assigning" />
      </div>
    </Dialog>

    <!-- 퀴즈 선택기 다이얼로그 -->
    <Dialog v-model:visible="showQuizPicker" header="퀴즈 선택" :style="{ width: '50rem' }" :modal="true" :draggable="false">
      <div class="p-4">
        <div class="search-bar-wide w-full mb-4">
          <span class="p-input-icon-right search-input-wrapper w-full">
            <InputText v-model="pickerKeyword" placeholder="제목으로 검색..." class="search-input w-full" @keyup.enter="loadPickerQuizzes" />
            <i class="pi pi-search search-icon" @click="loadPickerQuizzes"></i>
          </span>
        </div>

        <div v-if="pickerLoading" class="text-center py-8">
          <i class="pi pi-spin pi-spinner text-3xl"></i>
        </div>
        <div v-else-if="pickerQuizzes.length === 0" class="text-center py-8">
          <p class="text-secondary">퀴즈가 없습니다.</p>
        </div>
        <div v-else class="quiz-list-compact">
          <div
            v-for="quiz in pickerQuizzes"
            :key="quiz.id"
            class="quiz-list-item cursor-pointer hover:bg-surface-hover p-2 rounded-lg flex gap-3 items-center border-b border-surface-border"
            @click="selectQuiz(quiz)"
          >
            <img :src="resolveImageUrl(quiz.thumbnailUrl) || '/placeholder.svg'" class="w-16 h-16 object-cover rounded-md" />
            <div class="flex-1">
              <h4 class="font-bold text-lg m-0" style="color: var(--color-heading)">{{ quiz.title }}</h4>
              <span class="text-sm text-secondary">ID: {{ quiz.id }} | {{ quiz.author?.nickname }}</span>
            </div>
            <Button icon="pi pi-plus" text rounded />
          </div>
        </div>
      </div>
    </Dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive, watch, computed } from "vue";
import {
  getAdminQuizzes,
  toggleQuizVisibility,
  createChallenge,
  createCustomItem,
  getCustomItems,
  assignItemToUser,
  uploadItemImage,
  updateCustomItem,
  getAssignedUsers,
  revokeUserItem,
  deleteCustomItem,
} from "@/api/admin";
import { resolveImageUrl } from "@/lib/image";
import ProfileBackground from "@/components/user/ProfileBackground.vue"; // Keeping for reference if needed, but likely replaced
import ProfileHeader from "@/components/user/ProfileHeader.vue";
import UserPopoverCard from "@/components/common/UserPopoverCard.vue";
import Button from "primevue/button";
import InputText from "primevue/inputtext";
import InputNumber from "primevue/inputnumber";
import Textarea from "primevue/textarea";
import RadioButton from "primevue/radiobutton";
import Checkbox from "primevue/checkbox";
import Select from "primevue/select";
import DatePicker from "primevue/datepicker";
import Paginator from "primevue/paginator";
import Dialog from "primevue/dialog";
import { useToast } from "primevue/usetoast";
import ColorPicker from "primevue/colorpicker";
import Slider from "primevue/slider";
import ToggleSwitch from "primevue/toggleswitch";
import MultiSelect from "primevue/multiselect";

const toast = useToast();
const activeTab = ref("quizzes");
const loading = ref(false);

const submitting = ref(false);

// --- 커스텀 아이템 로직 ---
const customItems = ref([]);
const newItem = reactive({
  itemType: "THEME",
  name: "",
  description: "",
  configStr: "",
  isDefault: false,
});

const defaultConfigs = {
  THEME: {
    style: {
      color: "#ffffff",
      textShadow: "1px 1px 2px rgba(0,0,0,0.5)",
    },
    textColor: "#ffffff",
  },
  POPOVER: {
    style: {
      border: "1px solid #e2e8f0",
      boxShadow: "0 4px 6px -1px rgba(0, 0, 0, 0.1)",
    },
    textColor: "#000000",
    overlayStyle: "bg-white/90 backdrop-blur-sm",
  },
};

// 타입 변경 시 설정 JSON 자동 채움
watch(
  () => newItem.itemType,
  (newType) => {
    // 유저의 커스텀 수정을 덮어쓰지 않도록 비어있을 때만 설정
    if (!newItem.configStr) {
      newItem.configStr = JSON.stringify(defaultConfigs[newType] || {}, null, 2);
    }
  },
  { immediate: true }
);

// --- 비주얼 빌더 상태 ---
const useBuilder = ref(true); // 빌더 모드 기본값
const builderForm = reactive({
  backgroundColor: "255, 255, 255", // RGB for ColorPicker (PrimeVue uses HEX by default? No, ColorPicker default is HEX but can be configurable. Let's assume HEX)
  // Actually PrimeVue ColorPicker default is HEX string if format not specified?
  // Let's check docs or assume standard. Usually returns hex string like 'ff0000'.
  bgColor: "ffffff",
  textColor: "000000",
  borderColor: "cccccc",
  borderWidth: 1,
  shadowColor: "000000",
  animationNames: [], // Changed from animationName string to array
  animationDuration: 2,
  useGradient: false,
  gradientStart: "ffffff",
  gradientEnd: "000000",
});

const animationOptions = ref([
  { label: "없음", value: "none" },
  // Attention
  { label: "바운스 (Bounce)", value: "bounce" },
  { label: "펄스 (Pulse)", value: "pulse" },
  { label: "흔들기 (Shake)", value: "shake" },
  { label: "젤로 (Jello)", value: "jello" },
  { label: "러버밴드 (RubberBand)", value: "rubberBand" },
  // Continuous
  { label: "숨쉬기 (Breathing)", value: "breathing" },
  { label: "회전 (Rotate)", value: "rotate-slow" },
  { label: "스윙 (Swing)", value: "swing" },
  { label: "둥둥 떠다니기 (Floating)", value: "floating" },
  // Special
  { label: "네온 글로우 (Neon)", value: "neon-pulse" }, // utilizing existing class logic or keyframe
  { label: "무지개 테두리 (Rainbow)", value: "rainbow-border" },
  { label: "글리치 (Glitch)", value: "glitch-skew" },
  { label: "빛나는 효과 (Shine)", value: "shine" },
  { label: "테두리 댄스 (Border Dance)", value: "border-dance" },
]);

// 빌더 -> 설정 문자열 & 미리보기 자동 동기화
watch(
  builderForm,
  () => {
    // 빌더 모드/수정 중일 때만 생성.
    // 토글을 제거했으므로 기본적으로 빌더를 사용한다고 가정.
    generateConfigFromBuilder();
  },
  { deep: true }
);

// 설정 문자열 -> 미리보기 자동 동기화 (직접 JSON 편집 또는 빌더 업데이트 반응)
watch(
  () => newItem.configStr,
  () => {
    applyPreview();
  }
);

// 모드 전환 또는 로딩 시 JSON -> 빌더 자동 동기화
const parseConfigToBuilder = () => {
  // 루프 방지를 위해 빌더에서 생성한 경우 재파싱 방지?
  // 사실 JSON을 입력하면 빌더가 업데이트되길 원함.
  // 이상적으로는 변경 소스를 추적해야 함.

  if (!newItem.configStr) return;
  try {
    const config = JSON.parse(newItem.configStr);
    const style = config.style || {};

    // 배경
    if (style.background && style.background.includes("linear-gradient")) {
      builderForm.useGradient = true;
      // 최선의 추출 노력: linear-gradient(45deg, #ffffff, #000000)
      const matches = style.background.match(/#([a-fA-F0-9]{3,6})/g);
      if (matches && matches.length >= 2) {
        builderForm.gradientStart = matches[0].replace("#", "");
        builderForm.gradientEnd = matches[1].replace("#", "");
      }
    } else if (style.backgroundColor) {
      builderForm.useGradient = false;
      builderForm.bgColor = style.backgroundColor.replace("#", "");
    }

    // 텍스트 색상
    if (config.textColor) {
      builderForm.textColor = config.textColor.replace("#", "");
    }

    // 테두리
    if (style.border && style.border !== "none") {
      // "1px solid #cccccc"
      const parts = style.border.split(" ");
      if (parts.length >= 3) {
        builderForm.borderWidth = parseInt(parts[0]);
        builderForm.borderColor = parts[2].replace("#", "");
      }
    } else {
      builderForm.borderWidth = 0;
    }

    // 애니메이션
    if (style.animation) {
      // "bounce 2s infinite linear, pulse 2s infinite linear"
      // Split by comma
      const anims = style.animation.split(",");
      const names = [];
      let duration = 2;

      anims.forEach((animStr) => {
        const parts = animStr.trim().split(" ");
        if (parts.length > 0) {
          const name = parts[0];
          if (name !== "gradient-flow") {
            // 내부 사용
            names.push(name);
          }
          if (parts.length > 1) {
            const d = parseFloat(parts[1]);
            if (!isNaN(d)) duration = d;
          }
        }
      });
      builderForm.animationNames = names;
      builderForm.animationDuration = duration;
    } else {
      builderForm.animationNames = [];
    }
  } catch (e) {
    console.warn("Failed to parse config to builder", e);
  }
};

watch(useBuilder, (val) => {
  if (val) {
    parseConfigToBuilder();
  }
});

const generateConfigFromBuilder = () => {
  // 빌더 상태를 기반으로 설정 객체 구성
  const style = {};
  const textStyle = {}; // 일부 애니메이션/스타일은 텍스트에 적용됨

  // 배경 - 이미지가 업로드되지 않은 경우 색상 강제
  // uploadedImageUrl과 localPreviewUrl 모두 확인
  if (!uploadedImageUrl.value && !localPreviewUrl.value) {
    if (builderForm.useGradient) {
      style.background = `linear-gradient(45deg, #${builderForm.gradientStart}, #${builderForm.gradientEnd}) !important`;
      style.backgroundSize = "200% 200%";
      style.animation = (!builderForm.animationNames || builderForm.animationNames.length === 0 || builderForm.animationNames.includes('none')) ? "gradient-flow 3s ease infinite" : style.animation;
    } else {
      style.backgroundColor = `#${builderForm.bgColor} !important`;
    }
  }

  // 테두리
  if (builderForm.borderWidth > 0) {
    style.border = `${builderForm.borderWidth}px solid #${builderForm.borderColor} !important`;
  } else {
    style.border = "none !important";
  }

  // Animation
  // 애니메이션
  // 쉼표로 연결된 다중 애니메이션
  if (builderForm.animationNames && builderForm.animationNames.length > 0) {
    const duration = builderForm.animationDuration || 2;

    const animStrings = builderForm.animationNames
      .map((name) => {
        if (name === "none") return null;

        let timing = "linear";
        if (["bounce", "pulse", "shake", "swing", "rubberBand", "jello"].includes(name)) {
          timing = "ease-in-out";
        }
        return `${name} ${duration}s infinite ${timing}`;
      })
      .filter((s) => s !== null);

    if (animStrings.length > 0) {
      // Should valid CSS animation syntax: anim1, anim2
      style.animation = animStrings.join(", ") + " !important";
    }
  }

  const config = {
    style: style,
    textColor: `#${builderForm.textColor}`,
    // 수정: DB 저장을 방지하기 위해 configStr에 localPreviewUrl (Blob) 포함 안 함.
    // applyPreview()가 Blob의 시각적 미리보기를 별도로 처리함.
    image: uploadedImageUrl.value || null,
  };

  newItem.configStr = JSON.stringify(config, null, 2);
  // applyPreview(); // handled by watch(newItem.configStr)
};

// 퀴즈 탭 상태
const quizzes = ref([]);
const searchKeyword = ref("");
const page = ref(1);
const size = ref(21);

// 퀴즈 선택기 상태
const showQuizPicker = ref(false);
const pickerQuizzes = ref([]);
const pickerKeyword = ref("");
const pickerLoading = ref(false);
const selectedQuiz = ref(null);

// 챌린지 탭 상태
const challengeForm = reactive({
  title: "",
  description: "",
  challengeType: "SURVIVAL",
  targetQuizId: null,
  timeLimit: null,
  startAt: null,
  endAt: null,
});

// --- 메인 퀴즈 목록 ---
const totalRecords = ref(0);


const loadQuizzes = async () => {
  loading.value = true;
  try {
    const data = await getAdminQuizzes({
      page: page.value,
      size: size.value,
      keyword: searchKeyword.value,
    });
    // 백엔드는 이제 PageResponse { content, totalElements, ... }를 반환함
    if (data && data.content) {
      quizzes.value = data.content;
      totalRecords.value = data.totalElements;
    } else {
      // 폴백 또는 비어있음
      quizzes.value = [];
      totalRecords.value = 0;
    }
  } catch (error) {
    toast.add({ severity: "error", summary: "오류", detail: "퀴즈 목록을 불러오지 못했습니다.", life: 3000 });
  } finally {
    loading.value = false;
  }
};

const handleToggleHide = async (quiz) => {
  try {
    await toggleQuizVisibility(quiz.id);
    quiz.hidden = !quiz.hidden;
    const msg = quiz.hidden ? "퀴즈가 숨김 처리되었습니다." : "퀴즈 숨김이 해제되었습니다.";
    toast.add({ severity: "success", summary: "성공", detail: msg, life: 3000 });
  } catch (error) {
    toast.add({ severity: "error", summary: "오류", detail: "상태 변경에 실패했습니다.", life: 3000 });
  }
};

const onPageChange = (event) => {
  page.value = event.page + 1;
  loadQuizzes();
};

// --- 퀴즈 선택기 ---
const openQuizPicker = () => {
  showQuizPicker.value = true;
  if (pickerQuizzes.value.length === 0) {
    loadPickerQuizzes();
  }
};

const loadPickerQuizzes = async () => {
  pickerLoading.value = true;
  try {
    const data = await getAdminQuizzes({
      page: 1,
      size: 50, // 선택기를 위해 더 많이 로드
      keyword: pickerKeyword.value,
    });
    // API가 반환하는 경우 선택기에 대해서도 PageResponse 구조 처리
    if (data && data.content) {
      pickerQuizzes.value = data.content;
    } else if (Array.isArray(data)) {
      pickerQuizzes.value = data;
    }
  } catch (error) {
    // 조용히 또는 토스트
  } finally {
    pickerLoading.value = false;
  }
};

const selectQuiz = (quiz) => {
  selectedQuiz.value = quiz;
  challengeForm.targetQuizId = quiz.id;
  showQuizPicker.value = false;
};

const clearSelectedQuiz = () => {
  selectedQuiz.value = null;
  challengeForm.targetQuizId = null;
};

// --- 챌린지 제출 ---
const submitChallenge = async () => {
  // 수동 유효성 검사
  if (!challengeForm.targetQuizId) {
    toast.add({ severity: "warn", summary: "알림", detail: "대상 퀴즈를 선택해주세요.", life: 3000 });
    return;
  }
  if (!challengeForm.title) {
    toast.add({ severity: "warn", summary: "알림", detail: "제목을 입력해주세요.", life: 3000 });
    return;
  }

  submitting.value = true;
  try {
    await createChallenge(challengeForm);
    toast.add({ severity: "success", summary: "성공", detail: "챌린지가 성공적으로 생성되었습니다.", life: 3000 });
    // 폼 초기화
    challengeForm.title = "";
    challengeForm.description = "";
    clearSelectedQuiz();
    challengeForm.timeLimit = null;
    challengeForm.startAt = null;
    challengeForm.endAt = null;
  } catch (error) {
    toast.add({ severity: "error", summary: "오류", detail: "챌린지 생성에 실패했습니다.", life: 3000 });
  } finally {
    submitting.value = false;
  }
};

// --- 커스텀 아이템 로직 변수 상단으로 이동됨 ---

// Watcher 상단으로 이동됨

const creatingItem = ref(false);
const showAssignDialog = ref(false);
const selectedItemToAssign = ref(null);
const assignUserId = ref(null);
const assignedUsers = ref([]);

const assigning = ref(false); // 할당 상태 복원
const uploadedImageUrl = ref(""); // uploadedImageUrl 복원
const localPreviewUrl = ref(""); // 즉시 미리보기용
// --- 수정 & 유저 관리 로직 정의 (호이스팅됨) ---
const isEditMode = ref(false);
const editingItemId = ref(null);
const showUserDialog = ref(false);
const loadingUsers = ref(false);

watch(localPreviewUrl, () => {
  // 이미지가 변경되면 '비활성화된' 배경 로직을 적용하기 위해 설정 재생성
  generateConfigFromBuilder();
});
const fileInput = ref(null);

const triggerFileUpload = () => {
  fileInput.value.click();
};

const loadCustomItems = async () => {
  try {
    const data = await getCustomItems();
    customItems.value = data;
  } catch (e) {
    console.error(e);
  }
};

const currentPreviewConfig = ref({ image: null });

// 미리보기 로직 적용
const applyPreview = () => {
  try {
    const config = newItem.configStr ? JSON.parse(newItem.configStr) : {};

    // 이미지가 존재하고 수동으로 설정되지 않은 경우 설정에 주입 (또는 강제 덮어쓰기/병합?)
    // 병합: 업로드된 이미지가 존재하면 미리보기 편의를 위해 'image' 필드 우선순위
    if (localPreviewUrl.value) {
      config.image = localPreviewUrl.value;
    } else if (uploadedImageUrl.value) {
      config.image = uploadedImageUrl.value;
    }

    currentPreviewConfig.value = config;

    // 이미지 경로가 없다면 JSON 텍스트 박스도 업데이트해서 반영할까?
    // 분리하는 게 나을 수도 있지만, 보통 유저는 JSON에서 경로를 보고 싶어함.
    // 유저 수정을 덮어쓰지 않기 위해 미리보기 시 JSON 텍스트 자동 업데이트 금지,
    // 하지만 업로드 완료 시에는 자동 업데이트했음.
  } catch (e) {
    toast.add({ severity: "error", summary: "JSON 오류", detail: "설정 JSON 형식이 올바르지 않습니다.", life: 3000 });
    currentPreviewConfig.value = { image: null };
  }
};

// 파일 업로드 처리
const onFileSelect = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  // 즉시 로컬 미리보기
  localPreviewUrl.value = URL.createObjectURL(file);
  // Immediate local preview
  localPreviewUrl.value = URL.createObjectURL(file);
  // applyPreview(); // 이제 localPreviewUrl/configStr 감시자가 명시적 업데이트 처리

  try {
    creatingItem.value = true; // 로딩 상태 사용
    const path = await uploadItemImage(file, newItem.itemType);
    uploadedImageUrl.value = path;

    // JSON 설정에 자동 주입
    let config = {};
    try {
      config = newItem.configStr ? JSON.parse(newItem.configStr) : {};
    } catch (e) {
      /* ignore */
    }

    config.image = path; // 이미지 경로 설정
    newItem.configStr = JSON.stringify(config, null, 2);

    // 미리보기 업데이트 트리거 (로컬 blob이 실패했거나 설정 일관성을 보장하기 위해 경로로 폴백)
    // 페이지 새로고침이나 초기화 전까지 표시를 위해 localPreviewUrl 유지

    toast.add({ severity: "info", summary: "업로드 완료", detail: "이미지 경로가 설정에 추가되었습니다.", life: 3000 });
  } catch (e) {
    toast.add({ severity: "error", summary: "업로드 실패", detail: "이미지 업로드 중 오류가 발생했습니다.", life: 3000 });
  } finally {
    creatingItem.value = false;
    // 파일 입력 초기화
    if (fileInput.value) fileInput.value.value = "";
  }
};

const removeImage = () => {
  uploadedImageUrl.value = "";
  localPreviewUrl.value = "";

  // 색상 복원을 위해 설정 재생성 트리거
  generateConfigFromBuilder();

  toast.add({ severity: "info", summary: "삭제 완료", detail: "이미지가 삭제되었습니다.", life: 3000 });
};

const createItem = async () => {
  if (!newItem.configStr) return;
  try {
    creatingItem.value = true;
    let configObj = {};
    try {
      configObj = JSON.parse(newItem.configStr);
    } catch (e) {
      toast.add({ severity: "error", summary: "JSON 오류", detail: "유효한 JSON 형식이 아닙니다.", life: 3000 });
      return;
    }

    await createCustomItem({
      itemType: newItem.itemType,
      name: newItem.name,
      description: newItem.description,
      config: configObj,
      isDefault: newItem.isDefault,
    });

    toast.add({ severity: "success", summary: "생성 완료", detail: "새로운 아이템이 생성되었습니다.", life: 3000 });

    // 초기화 및 재로딩
    isEditMode.value = false;
    newItem.itemType = "THEME";
    newItem.name = "";
    newItem.description = "";
    newItem.configStr = "";
    newItem.isDefault = false;
    newItem.configStr = "";
    newItem.isDefault = false;
    uploadedImageUrl.value = "";
    localPreviewUrl.value = "";
    currentPreviewConfig.value = { image: null };

    await loadCustomItems();
  } catch (e) {
    console.error(e);
    toast.add({ severity: "error", summary: "생성 실패", detail: "아이템 생성 중 오류가 발생했습니다.", life: 3000 });
  } finally {
    creatingItem.value = false;
  }
};

// --- 수정 & 유저 관리 로직 ---
// 호이스팅 안전을 위해 변수 상단으로 이동

const startEdit = (item) => {
  isEditMode.value = true;
  editingItemId.value = item.id;
  newItem.itemType = item.itemType;
  newItem.name = item.name;
  newItem.description = item.description;
  newItem.isDefault = item.isDefault;

  // 설정은 API에서 객체(매퍼/서비스에서 매핑됨) 또는 문자열로 오나?
  // getAllItems 매퍼 결과에서 설정은 문자열(JSON)임.
  newItem.configStr = item.config;

  // 가능하면 미리보기용 이미지 결정
  try {
    const conf = JSON.parse(item.config);
    if (conf.image) uploadedImageUrl.value = conf.image;
  } catch (e) {
    /* ignore */
  }

  // 빌더를 위한 초기 파싱
  if (useBuilder.value) {
    parseConfigToBuilder();
  }

  applyPreview();

  // 맨 위로 스크롤
  window.scrollTo({ top: 0, behavior: "smooth" });
};

const cancelEdit = () => {
  isEditMode.value = false;
  editingItemId.value = null;
  newItem.name = "";
  newItem.description = "";
  newItem.configStr = "";
  newItem.isDefault = false;
  newItem.isDefault = false;
  uploadedImageUrl.value = "";
  localPreviewUrl.value = "";
  currentPreviewConfig.value = { image: null };
};

const updateItem = async () => {
  if (!newItem.configStr) return;
  try {
    creatingItem.value = true;
    let configObj = {};
    try {
      configObj = JSON.parse(newItem.configStr);
    } catch (e) {
      toast.add({ severity: "error", summary: "JSON 오류", detail: "유효한 JSON 형식이 아닙니다.", life: 3000 });
      return;
    }

    await updateCustomItem(editingItemId.value, {
      itemType: newItem.itemType,
      name: newItem.name,
      description: newItem.description,
      config: configObj,
      isDefault: newItem.isDefault,
    });

    toast.add({ severity: "success", summary: "수정 완료", detail: "아이템이 수정되었습니다.", life: 3000 });

    cancelEdit(); // 초기화
    await loadCustomItems();
  } catch (e) {
    toast.add({ severity: "error", summary: "수정 실패", detail: "아이템 수정 중 오류가 발생했습니다.", life: 3000 });
  } finally {
    creatingItem.value = false;
  }
};

const openUserDialog = async (item) => {
  selectedItemToAssign.value = item;
  showUserDialog.value = true;
  loadingUsers.value = true;
  try {
    assignedUsers.value = await getAssignedUsers(item.id);
  } catch (e) {
    toast.add({ severity: "error", summary: "오류", detail: "유저 목록을 불러오지 못했습니다." });
  } finally {
    loadingUsers.value = false;
  }
};

const removeUser = async (userId) => {
  try {
    await revokeUserItem(selectedItemToAssign.value.id, userId);
    toast.add({ severity: "success", summary: "해제 완료", detail: "유저 아이템 할당이 해제되었습니다." });
    // 목록 새로고침
    assignedUsers.value = assignedUsers.value.filter((u) => u.id !== userId);
  } catch (e) {
    toast.add({ severity: "error", summary: "오류", detail: "할당 해제 실패" });
  }
};

const openAssignDialog = (item) => {
  selectedItemToAssign.value = item;
  assignUserId.value = null;
  showAssignDialog.value = true;
};

const assignItem = async () => {
  if (!assignUserId.value) return;
  try {
    assigning.value = true;
    await assignItemToUser(selectedItemToAssign.value.id, assignUserId.value);
    toast.add({ severity: "success", summary: "성공", detail: "할당 완료", life: 3000 });
    showAssignDialog.value = false;
  } catch (e) {
    toast.add({ severity: "error", summary: "오류", detail: "할당 실패 (이미 보유중이거나 유저 없음)", life: 3000 });
  } finally {
    assigning.value = false;
  }
};

const confirmDelete = async (item) => {
  if (!confirm(`'${item.name}' 아이템을 정말 삭제하시겠습니까?`)) return;

  try {
    await deleteCustomItem(item.id);
    toast.add({ severity: "success", summary: "삭제 완료", detail: "아이템이 삭제되었습니다." });
    await loadCustomItems(); // 목록 새로고침
  } catch (e) {
    toast.add({ severity: "error", summary: "오류", detail: "삭제 실패" });
  }
};

// 아이템 로드를 위해 탭 감시
watch(activeTab, (val) => {
  if (val === "custom-items") {
    loadCustomItems();
  }
});

onMounted(() => {
  loadQuizzes();
});
</script>

<style scoped>
/* 전역 레이아웃 */
.admin-container {
  min-height: calc(100vh - 200px);
  padding: 1rem 0 3rem;
  color: var(--color-text);
}

.container {
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  color: var(--color-heading);
  margin-bottom: 2rem;
  text-align: center;
}

/* 탭 */
.tab-navbar {
  display: flex;
  gap: 0;
  border-bottom: 2px solid var(--surface-border);
  margin-bottom: 2rem;
  /* justify-content: center; 전체 너비 채우기를 위해 제거됨 */
  background: var(--surface-ground);
}

.tab-button {
  flex: 1;
  /* max-width: 200px; 전체 너비 채우기를 위해 제거됨 */
  padding: 1rem 1.5rem;
  background: transparent;
  border: none;
  border-bottom: 3px solid transparent;
  cursor: pointer;
  font-size: 1rem;
  font-weight: 500;
  color: var(--text-color-secondary);
  transition: all 0.3s ease;
  border-radius: 12px 12px 0 0;
}

/* ... 기존 호버/활성 스타일 ... */
.tab-button:hover {
  background: var(--surface-hover);
  color: var(--text-color);
}

.tab-button.active {
  color: var(--primary-color);
  border-bottom-color: var(--primary-color);
  font-weight: 600;
}

/* 검색 바 위치 지정 */
.search-input-wrapper {
  position: relative;
  display: block;
}

.search-icon {
  position: absolute;
  right: 1rem;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-color-secondary);
  cursor: pointer;
}

/* 탭 콘텐츠 애니메이션 */
.tab-content {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 퀴즈 그리드 (마이페이지와 일치) */
.quiz-grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); /* 반응형 유지 */
  gap: 1rem;
  padding: 0.5rem;
}

/* 퀴즈 카드 (마이페이지와 일치) */
.quiz-card {
  background: var(--color-background-soft);
  border-radius: 18px;
  border: 1px solid var(--color-border);
  padding: 0.6rem 0.6rem 0.3rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  box-shadow: var(--surface-glow);
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: visible; /* 필요 시 조정 */
}

.quiz-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.quiz-card:hover .quiz-thumbnail-img {
  transform: scale(1.03);
}

.quiz-thumbnail {
  width: 100%;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 14px;
  background: var(--surface-section);
  position: relative;
}

.quiz-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s ease;
}

.hidden-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: bold;
  z-index: 2;
}

.quiz-body {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  padding: 0 0.4rem 0.4rem;
}

.quiz-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem;
}

.quiz-title {
  font-size: 1rem;
  font-weight: 700;
  margin: 0;
  color: var(--color-heading);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.default-item-badge {
  font-weight: 600;
  color: var(--text-primary);
}

.quiz-stat {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.3rem 0.6rem;
  border-radius: 999px;
  background: var(--primary-50);
  color: var(--color-heading);
  font-size: 0.85rem;
}

.quiz-actions {
  display: flex;
  gap: 0.5rem;
}

/* 폼 */
.challenge-form-card {
  background: var(--color-background-soft);
  border: 1px solid var(--color-border);
  border-radius: 18px;
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
  box-shadow: var(--surface-glow);
}

.form-title {
  color: var(--color-heading);
  margin-bottom: 1.5rem;
  text-align: center;
}

.field {
  display: flex;
  flex-direction: column;
}

.field-label {
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--color-heading);
}

.field-label-text {
  color: var(--color-text);
}

.text-color {
  color: var(--color-text);
}

.text-secondary {
  color: var(--color-text-muted);
}

/* 입력 스타일 */
.search-input {
  background: var(--color-background-soft);
  border-color: var(--color-border);
  color: var(--color-text);
}

/* 선택기 목록 */
.quiz-list-compact {
  max-height: 400px;
  overflow-y: auto;
}
.quiz-list-item {
  transition: background-color 0.2s;
}
.quiz-list-item:hover {
  background-color: var(--color-background-mute);
}
</style>
