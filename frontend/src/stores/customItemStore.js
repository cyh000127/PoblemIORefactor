import { defineStore } from 'pinia';
import { ref } from 'vue';
import axios from '@/api/axios';
import { AVATAR_DECORATIONS } from '@/constants/avatarConfig';
import { POPOVER_DECORATIONS } from '@/constants/popoverConfig';
import { PROFILE_THEMES } from '@/constants/themeConfig';

export const useCustomItemStore = defineStore('customItem', () => {
    // 상태 (State)
    const avatarItems = ref([]);
    const popoverItems = ref([]);
    const themeItems = ref([]);
    const loading = ref(false);

    // 모든 아이템 딕셔너리 (DB ID 기준): { [id]: mergedConfig }
    const itemDefinitions = ref({});

    const safeParseConfig = (config, itemForLog) => {
        if (!config) return {};
        if (typeof config === 'object') return config;

        if (typeof config === 'string') {
            try {
                return JSON.parse(config);
            } catch (e) {
                console.error('Failed to parse item config', itemForLog);
                return {};
            }
        }
        return {};
    };

    // 로컬 상수로 특정 아이템(예: Cybercity) override/보정
    const applyLocalOverrides = (itemType, itemName, baseConfig) => {
        if (itemName !== 'Cybercity') return baseConfig;

        if (itemType === 'POPOVER' && POPOVER_DECORATIONS.cybercity) {
            return { ...baseConfig, ...POPOVER_DECORATIONS.cybercity };
        }
        if (itemType === 'THEME' && PROFILE_THEMES.cybercity) {
            return { ...baseConfig, ...PROFILE_THEMES.cybercity };
        }
        return baseConfig;
    };

    // 액션 (Actions)
    const fetchItemDefinitions = async () => {
        try {
            const res = await axios.get('/items/definitions');

            const defs = {};
            (res.data || []).forEach((item) => {
                let config = safeParseConfig(item.config, item);
                config = applyLocalOverrides(item.itemType, item.name, config);

                defs[item.id] = {
                    id: item.id,
                    name: item.name,
                    itemType: item.itemType,
                    ...config
                };
            });

            itemDefinitions.value = defs;
        } catch (e) {
            console.error('Failed to fetch item definitions', e);
        }
    };

    const fetchUserItems = async () => {
        loading.value = true;

        // definitions가 비어있으면 먼저 로드 (관리자 미리보기 / 설정 병합용)
        if (Object.keys(itemDefinitions.value).length === 0) {
            await fetchItemDefinitions();
        }

        try {
            const [avatars, popovers, themes] = await Promise.all([
                axios.get('/items/my', { params: { type: 'AVATAR' } }),
                axios.get('/items/my', { params: { type: 'POPOVER' } }),
                axios.get('/items/my', { params: { type: 'THEME' } })
            ]);

            avatarItems.value = formatMyItems(avatars.data, 'AVATAR', AVATAR_DECORATIONS);
            popoverItems.value = formatMyItems(popovers.data, 'POPOVER', POPOVER_DECORATIONS);
            themeItems.value = formatMyItems(themes.data, 'THEME', PROFILE_THEMES);
        } catch (error) {
            console.error('Failed to fetch user items', error);

            // 정적 기본값으로 대체 (Fallback)
            avatarItems.value = formatMyItems([], 'AVATAR', AVATAR_DECORATIONS);
            popoverItems.value = formatMyItems([], 'POPOVER', POPOVER_DECORATIONS);
            themeItems.value = formatMyItems([], 'THEME', PROFILE_THEMES);
        } finally {
            loading.value = false;
        }
    };

    const formatMyItems = (backendItems, type, staticDefaults) => {
        const result = [];
        const added = new Set();

        // 1) 정적 기본값 (항상 보이도록 먼저 주입)
        if (staticDefaults) {
            Object.keys(staticDefaults).forEach((key) => {
                if (added.has(key)) return;

                result.push({
                    key,
                    id: key,          // 정적 아이템은 key를 id로 사용
                    isDefault: true,
                    isOwned: true,
                    ...staticDefaults[key]
                });
                added.add(key);
            });
        }

        // 2) 백엔드 아이템 (정적 뒤에 추가)
        (backendItems || []).forEach((item) => {
            const idKey = String(item.id);
            if (added.has(item.id) || added.has(idKey)) return;

            const parsed = safeParseConfig(item.config, item);
            const def = itemDefinitions.value[item.id] || itemDefinitions.value[idKey] || null;

            // def(=definitions) 쪽에 로컬 override가 들어갈 수 있으므로 def가 있으면 def가 우선권 가지게 구성
            const mergedConfig = def ? { ...parsed, ...def } : parsed;

            result.push({
                id: item.id,
                name: item.name,
                description: item.description,
                isDefault: item.isDefault,
                isOwned: true,
                ...mergedConfig
            });

            added.add(item.id);
            added.add(idKey);
        });

        return result;
    };

    // 키로 설정 가져오기 헬퍼 (정적 키와 숫자/문자 ID 모두 작동)
    const getItemConfig = (type, key) => {
        if (key === null || key === undefined) return null;

        const k = typeof key === 'string' ? key : String(key);

        // 1) 동적 정의 우선
        if (itemDefinitions.value[key]) return itemDefinitions.value[key];
        if (itemDefinitions.value[k]) return itemDefinitions.value[k];

        // 2) 정적 폴백
        if (type === 'AVATAR') return AVATAR_DECORATIONS[k] || AVATAR_DECORATIONS[key];
        if (type === 'POPOVER') return POPOVER_DECORATIONS[k] || POPOVER_DECORATIONS[key];
        if (type === 'THEME') return PROFILE_THEMES[k] || PROFILE_THEMES[key];

        return null;
    };

    return {
        avatarItems,
        popoverItems,
        themeItems,
        fetchUserItems,
        fetchItemDefinitions,
        getItemConfig,
        loading
    };
});
