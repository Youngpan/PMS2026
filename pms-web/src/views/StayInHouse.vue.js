import { onMounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useRouter } from 'vue-router';
import { stayApi, folioApi } from '@/api/modules';
const list = ref([]);
const router = useRouter();
async function load() { list.value = await stayApi.inHouse(); }
async function openFolio(row) {
    const f = await folioApi.byStay(row.id);
    router.push(`/folios/${f.folio.id}`);
}
function transfer(row) {
    ElMessageBox.prompt('目标房号ID', '换房', { inputPattern: /^\d+$/, inputErrorMessage: '请输入数字' })
        .then(async ({ value }) => {
        await stayApi.transferRoom(row.id, Number(value), '前台换房');
        ElMessage.success('已换房');
        load();
    }).catch(() => { });
}
function checkout(row) {
    ElMessageBox.prompt('实收金额', '退房收款', { inputPattern: /^\d+(\.\d+)?$/, inputErrorMessage: '请输入金额' })
        .then(async ({ value }) => {
        await stayApi.checkout(row.id, 'WECHAT', Number(value));
        ElMessage.success('已退房');
        load();
    }).catch(() => { });
}
onMounted(load);
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
const __VLS_0 = {}.ElCard;
/** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({}));
const __VLS_2 = __VLS_1({}, ...__VLS_functionalComponentArgsRest(__VLS_1));
var __VLS_4 = {};
__VLS_3.slots.default;
{
    const { header: __VLS_thisSlot } = __VLS_3.slots;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    const __VLS_5 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_6 = __VLS_asFunctionalComponent(__VLS_5, new __VLS_5({
        ...{ 'onClick': {} },
    }));
    const __VLS_7 = __VLS_6({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_6));
    let __VLS_9;
    let __VLS_10;
    let __VLS_11;
    const __VLS_12 = {
        onClick: (__VLS_ctx.load)
    };
    __VLS_8.slots.default;
    var __VLS_8;
}
const __VLS_13 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_14 = __VLS_asFunctionalComponent(__VLS_13, new __VLS_13({
    data: (__VLS_ctx.list),
    border: true,
    stripe: true,
}));
const __VLS_15 = __VLS_14({
    data: (__VLS_ctx.list),
    border: true,
    stripe: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_14));
__VLS_16.slots.default;
const __VLS_17 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
    prop: "id",
    label: "在住ID",
    width: "90",
}));
const __VLS_19 = __VLS_18({
    prop: "id",
    label: "在住ID",
    width: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_18));
const __VLS_21 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_22 = __VLS_asFunctionalComponent(__VLS_21, new __VLS_21({
    prop: "roomId",
    label: "房号ID",
    width: "90",
}));
const __VLS_23 = __VLS_22({
    prop: "roomId",
    label: "房号ID",
    width: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_22));
const __VLS_25 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_26 = __VLS_asFunctionalComponent(__VLS_25, new __VLS_25({
    prop: "guestId",
    label: "宾客ID",
    width: "90",
}));
const __VLS_27 = __VLS_26({
    prop: "guestId",
    label: "宾客ID",
    width: "90",
}, ...__VLS_functionalComponentArgsRest(__VLS_26));
const __VLS_29 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_30 = __VLS_asFunctionalComponent(__VLS_29, new __VLS_29({
    prop: "rate",
    label: "房价",
    width: "100",
}));
const __VLS_31 = __VLS_30({
    prop: "rate",
    label: "房价",
    width: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_30));
const __VLS_33 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_34 = __VLS_asFunctionalComponent(__VLS_33, new __VLS_33({
    prop: "checkInAt",
    label: "入住时间",
}));
const __VLS_35 = __VLS_34({
    prop: "checkInAt",
    label: "入住时间",
}, ...__VLS_functionalComponentArgsRest(__VLS_34));
const __VLS_37 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_38 = __VLS_asFunctionalComponent(__VLS_37, new __VLS_37({
    label: "操作",
    width: "280",
}));
const __VLS_39 = __VLS_38({
    label: "操作",
    width: "280",
}, ...__VLS_functionalComponentArgsRest(__VLS_38));
__VLS_40.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_40.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_41 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_42 = __VLS_asFunctionalComponent(__VLS_41, new __VLS_41({
        ...{ 'onClick': {} },
        size: "small",
    }));
    const __VLS_43 = __VLS_42({
        ...{ 'onClick': {} },
        size: "small",
    }, ...__VLS_functionalComponentArgsRest(__VLS_42));
    let __VLS_45;
    let __VLS_46;
    let __VLS_47;
    const __VLS_48 = {
        onClick: (...[$event]) => {
            __VLS_ctx.openFolio(row);
        }
    };
    __VLS_44.slots.default;
    var __VLS_44;
    const __VLS_49 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_50 = __VLS_asFunctionalComponent(__VLS_49, new __VLS_49({
        ...{ 'onClick': {} },
        size: "small",
        type: "warning",
    }));
    const __VLS_51 = __VLS_50({
        ...{ 'onClick': {} },
        size: "small",
        type: "warning",
    }, ...__VLS_functionalComponentArgsRest(__VLS_50));
    let __VLS_53;
    let __VLS_54;
    let __VLS_55;
    const __VLS_56 = {
        onClick: (...[$event]) => {
            __VLS_ctx.transfer(row);
        }
    };
    __VLS_52.slots.default;
    var __VLS_52;
    const __VLS_57 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_58 = __VLS_asFunctionalComponent(__VLS_57, new __VLS_57({
        ...{ 'onClick': {} },
        size: "small",
        type: "danger",
    }));
    const __VLS_59 = __VLS_58({
        ...{ 'onClick': {} },
        size: "small",
        type: "danger",
    }, ...__VLS_functionalComponentArgsRest(__VLS_58));
    let __VLS_61;
    let __VLS_62;
    let __VLS_63;
    const __VLS_64 = {
        onClick: (...[$event]) => {
            __VLS_ctx.checkout(row);
        }
    };
    __VLS_60.slots.default;
    var __VLS_60;
}
var __VLS_40;
var __VLS_16;
var __VLS_3;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            list: list,
            load: load,
            openFolio: openFolio,
            transfer: transfer,
            checkout: checkout,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
