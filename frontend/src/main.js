import "./assets/main.css";

import { createApp } from "vue";
import { createPinia } from "pinia";
import PrimeVue from "primevue/config";
import Aura from "@primevue/themes/aura";
import { definePreset } from "@primevue/themes";
import App from "./App.vue";
import router from "./router";

// PrimeVue 컴포넌트
import Button from "primevue/button";
import Card from "primevue/card";
import InputText from "primevue/inputtext";
import Password from "primevue/password";
import Textarea from "primevue/textarea";
import Dialog from "primevue/dialog";
import Toast from "primevue/toast";
import ToastService from "primevue/toastservice";
import ProgressBar from "primevue/progressbar";
import Avatar from "primevue/avatar";
import Badge from "primevue/badge";
import Divider from "primevue/divider";
import FileUpload from "primevue/fileupload";
import Menubar from "primevue/menubar";
import Chips from "primevue/chips";
import Paginator from "primevue/paginator";
import ConfirmDialog from "primevue/confirmdialog";
import ConfirmationService from "primevue/confirmationservice";
import Tooltip from "primevue/tooltip";

// PrimeIcons
import "primeicons/primeicons.css";

const app = createApp(App);
const pinia = createPinia();

// PrimeVue 설정
app.use(PrimeVue, {
  theme: {
    preset: definePreset(Aura, {
      semantic: {
        colorScheme: {
          light: {
            primary: {
              50: "#eifbfc",
              100: "#cff4f8",
              200: "#a5e9f2",
              300: "#6cd9e8",
              400: "#27b5cf", /* 선명한 시안(Cyan) 메인 */
              500: "#0ea0bc",
              600: "#10809a",
              700: "#13687f",
              800: "#185669",
              900: "#184859",
              950: "#0b2f3d",
              color: "{primary.500}",
              inverseColor: "#ffffff",
              hoverColor: "{primary.600}",
              activeColor: "{primary.700}",
            },
            highlight: {
              background: "{primary.50}",
              focusBackground: "{primary.100}",
              color: "{primary.700}",
              focusColor: "{primary.800}",
            },
          },
          dark: {
            primary: {
              50: "#eifbfc",
              100: "#cff4f8",
              200: "#a5e9f2",
              300: "#6cd9e8",
              400: "#27b5cf",
              500: "#0ea0bc",
              600: "#10809a",
              700: "#13687f",
              800: "#185669",
              900: "#184859",
              950: "#0b2f3d",
              color: "{primary.500}",
              inverseColor: "#ffffff",
              hoverColor: "{primary.600}",
              activeColor: "{primary.700}",
            },
            highlight: {
              background: "rgba(39, 181, 207, .16)",
              focusBackground: "rgba(39, 181, 207, .24)",
              color: "rgba(255,255,255,.87)",
              focusColor: "rgba(255,255,255,.87)",
            },
          },
        },
      },
    }),
    options: {
      darkModeSelector: ".dark-mode",
      cssLayer: false,
    },
  },
});

// [환경 변수 설정]
// 배경 이미지 설정 (public 폴더 내 이미지 사용)
// S3 관련 로직 제거됨 -> public/bg-light.png, public/bg-dark.png 사용
document.documentElement.style.setProperty('--app-bg-image-light', "url('/bg-light.png')");
document.documentElement.style.setProperty('--app-bg-image-dark', "url('/bg-dark.png')");

app.use(ToastService);
app.use(ConfirmationService);
app.use(pinia);
app.use(router);
app.directive("tooltip", Tooltip);

// PrimeVue 컴포넌트 등록
app.component("Button", Button);
app.component("Card", Card);
app.component("InputText", InputText);
app.component("Password", Password);
app.component("Textarea", Textarea);
app.component("Dialog", Dialog);
app.component("Toast", Toast);
app.component("ProgressBar", ProgressBar);
app.component("Avatar", Avatar);
app.component("Badge", Badge);
app.component("Divider", Divider);
app.component("FileUpload", FileUpload);
app.component("Menubar", Menubar);
app.component("Chips", Chips);
app.component("Paginator", Paginator);
app.component("ConfirmDialog", ConfirmDialog);

app.mount("#app");
