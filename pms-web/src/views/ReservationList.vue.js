import { onMounted, reactive, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { reservationApi, roomApi, stayApi } from '@/api/modules';
const list = ref([]);
const page = ref(1);
const pageSize = ref(20);
const total = ref(0);
const roomTypes = ref([]);
const dialogVisible = ref(false);
const form = reactive({ guestName: '', guestPhone: '', checkInDate: '', checkOutDate: '', roomTypeId: null, rate: 0, remark: '' });
async function load() {
    const res = await reservationApi.list({ page: page.value, page_size: pageSize.value });
    list.value = res.list || [];
    total.value = res.total || 0;
}
function openCreate() {
    Object.assign(form, { guestName: '', guestPhone: '', checkInDate: '', checkOutDate: '', roomTypeId: null, rate: 0, remark: '' });
    dialogVisible.value = true;
}
function onTypeChange() {
    const t = roomTypes.value.find(x => x.id === form.roomTypeId);
    if (t)
        form.rate = Number(t.baseRate);
}
async function create() {
    if (!form.roomTypeId) {
        ElMessage.warning('请选择房型');
        return;
    }
    await reservationApi.create({
        guestName: form.guestName,
        guestPhone: form.guestPhone,
        checkInDate: form.checkInDate,
        checkOutDate: form.checkOutDate,
        channelId: 1,
        remark: form.remark,
        rooms: [{ roomTypeId: form.roomTypeId, roomCount: 1, rate: form.rate }],
    });
    ElMessage.success('已创建');
    dialogVisible.value = false;
    page.value = 1;
    load();
}
async function confirm(row) {
    await reservationApi.confirm(row.id);
    ElMessage.success('已确认');
    load();
}
async function cancel(row) {
    await reservationApi.cancel(row.id);
    ElMessage.success('已取消');
    load();
}
async function checkin(row) {
    const detail = await reservationApi.get(row.id);
    const line = detail.rooms?.[0];
    if (!line) {
        ElMessage.warning('无预订房型明细');
        return;
    }
    const vacant = await roomApi.vacant(line.roomTypeId);
    if (!vacant.length) {
        ElMessage.warning('该房型无净房');
        return;
    }
    ElMessageBox.confirm(`可用净房：${vacant.map(r => r.number).join(', ')}，是否入住 ${vacant[0].number}？`, '办理入住')
        .then(async () => {
        await stayApi.checkin({
            reservationId: row.id,
            stayItems: [{ reservationRoomId: line.id, roomId: vacant[0].id }],
            deposit: 500, depositMethod: 'CASH',
        });
        ElMessage.success('已入住');
        load();
    }).catch(() => { });
}
function statusText(s) {
    return { PENDING: '待确认', CONFIRMED: '已确认', CHECKED_IN: '已入住', CHECKED_OUT: '已退房', CANCELLED: '已取消', NO_SHOW: '未到' }[s] || s;
}
onMounted(async () => {
    roomTypes.value = await roomApi.types();
    load();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
const __VLS_0 = {}.ElCard;
/** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({}));
const __VLS_2 = __VLS_1({}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
{
    const { header: __VLS_thisSlot } = __VLS_3.slots;
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({});
    const __VLS_4 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_6 = __VLS_5({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_5));
    let __VLS_8;
    let __VLS_9;
    let __VLS_10;
    const __VLS_11 = {
        onClick: (__VLS_ctx.openCreate)
    };
    __VLS_7.slots.default;
    var __VLS_7;
}
const __VLS_12 = {}.ElTable;
/** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    data: (__VLS_ctx.list),
    border: true,
    stripe: true,
}));
const __VLS_14 = __VLS_13({
    data: (__VLS_ctx.list),
    border: true,
    stripe: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
__VLS_15.slots.default;
const __VLS_16 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({
    prop: "resNo",
    label: "预订号",
    width: "150",
}));
const __VLS_18 = __VLS_17({
    prop: "resNo",
    label: "预订号",
    width: "150",
}, ...__VLS_functionalComponentArgsRest(__VLS_17));
const __VLS_20 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    prop: "guestName",
    label: "宾客",
    width: "100",
}));
const __VLS_22 = __VLS_21({
    prop: "guestName",
    label: "宾客",
    width: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
const __VLS_24 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_25 = __VLS_asFunctionalComponent(__VLS_24, new __VLS_24({
    prop: "guestPhone",
    label: "电话",
    width: "130",
}));
const __VLS_26 = __VLS_25({
    prop: "guestPhone",
    label: "电话",
    width: "130",
}, ...__VLS_functionalComponentArgsRest(__VLS_25));
const __VLS_28 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    prop: "checkInDate",
    label: "入住",
    width: "120",
}));
const __VLS_30 = __VLS_29({
    prop: "checkInDate",
    label: "入住",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
const __VLS_32 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_33 = __VLS_asFunctionalComponent(__VLS_32, new __VLS_32({
    prop: "checkOutDate",
    label: "离店",
    width: "120",
}));
const __VLS_34 = __VLS_33({
    prop: "checkOutDate",
    label: "离店",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_33));
const __VLS_36 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    prop: "nights",
    label: "晚数",
    width: "80",
}));
const __VLS_38 = __VLS_37({
    prop: "nights",
    label: "晚数",
    width: "80",
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
const __VLS_40 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_41 = __VLS_asFunctionalComponent(__VLS_40, new __VLS_40({
    prop: "status",
    label: "状态",
    width: "110",
}));
const __VLS_42 = __VLS_41({
    prop: "status",
    label: "状态",
    width: "110",
}, ...__VLS_functionalComponentArgsRest(__VLS_41));
__VLS_43.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_43.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_44 = {}.ElTag;
    /** @type {[typeof __VLS_components.ElTag, typeof __VLS_components.elTag, typeof __VLS_components.ElTag, typeof __VLS_components.elTag, ]} */ ;
    // @ts-ignore
    const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({}));
    const __VLS_46 = __VLS_45({}, ...__VLS_functionalComponentArgsRest(__VLS_45));
    __VLS_47.slots.default;
    (__VLS_ctx.statusText(row.status));
    var __VLS_47;
}
var __VLS_43;
const __VLS_48 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
    label: "操作",
    width: "240",
}));
const __VLS_50 = __VLS_49({
    label: "操作",
    width: "240",
}, ...__VLS_functionalComponentArgsRest(__VLS_49));
__VLS_51.slots.default;
{
    const { default: __VLS_thisSlot } = __VLS_51.slots;
    const [{ row }] = __VLS_getSlotParams(__VLS_thisSlot);
    const __VLS_52 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
        ...{ 'onClick': {} },
        size: "small",
        type: "success",
        disabled: (row.status !== 'PENDING'),
    }));
    const __VLS_54 = __VLS_53({
        ...{ 'onClick': {} },
        size: "small",
        type: "success",
        disabled: (row.status !== 'PENDING'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_53));
    let __VLS_56;
    let __VLS_57;
    let __VLS_58;
    const __VLS_59 = {
        onClick: (...[$event]) => {
            __VLS_ctx.confirm(row);
        }
    };
    __VLS_55.slots.default;
    var __VLS_55;
    const __VLS_60 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
        ...{ 'onClick': {} },
        size: "small",
        type: "warning",
        disabled: (row.status !== 'CONFIRMED'),
    }));
    const __VLS_62 = __VLS_61({
        ...{ 'onClick': {} },
        size: "small",
        type: "warning",
        disabled: (row.status !== 'CONFIRMED'),
    }, ...__VLS_functionalComponentArgsRest(__VLS_61));
    let __VLS_64;
    let __VLS_65;
    let __VLS_66;
    const __VLS_67 = {
        onClick: (...[$event]) => {
            __VLS_ctx.checkin(row);
        }
    };
    __VLS_63.slots.default;
    var __VLS_63;
    const __VLS_68 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
        ...{ 'onClick': {} },
        size: "small",
        type: "danger",
    }));
    const __VLS_70 = __VLS_69({
        ...{ 'onClick': {} },
        size: "small",
        type: "danger",
    }, ...__VLS_functionalComponentArgsRest(__VLS_69));
    let __VLS_72;
    let __VLS_73;
    let __VLS_74;
    const __VLS_75 = {
        onClick: (...[$event]) => {
            __VLS_ctx.cancel(row);
        }
    };
    __VLS_71.slots.default;
    var __VLS_71;
}
var __VLS_51;
var __VLS_15;
const __VLS_76 = {}.ElPagination;
/** @type {[typeof __VLS_components.ElPagination, typeof __VLS_components.elPagination, ]} */ ;
// @ts-ignore
const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
    ...{ 'onCurrentChange': {} },
    ...{ style: {} },
    currentPage: (__VLS_ctx.page),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
    layout: "prev, pager, next, total",
}));
const __VLS_78 = __VLS_77({
    ...{ 'onCurrentChange': {} },
    ...{ style: {} },
    currentPage: (__VLS_ctx.page),
    pageSize: (__VLS_ctx.pageSize),
    total: (__VLS_ctx.total),
    layout: "prev, pager, next, total",
}, ...__VLS_functionalComponentArgsRest(__VLS_77));
let __VLS_80;
let __VLS_81;
let __VLS_82;
const __VLS_83 = {
    onCurrentChange: (__VLS_ctx.load)
};
var __VLS_79;
var __VLS_3;
const __VLS_84 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({
    modelValue: (__VLS_ctx.dialogVisible),
    title: "新建预订",
    width: "480px",
}));
const __VLS_86 = __VLS_85({
    modelValue: (__VLS_ctx.dialogVisible),
    title: "新建预订",
    width: "480px",
}, ...__VLS_functionalComponentArgsRest(__VLS_85));
__VLS_87.slots.default;
const __VLS_88 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
    model: (__VLS_ctx.form),
    labelWidth: "80px",
}));
const __VLS_90 = __VLS_89({
    model: (__VLS_ctx.form),
    labelWidth: "80px",
}, ...__VLS_functionalComponentArgsRest(__VLS_89));
__VLS_91.slots.default;
const __VLS_92 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
    label: "姓名",
}));
const __VLS_94 = __VLS_93({
    label: "姓名",
}, ...__VLS_functionalComponentArgsRest(__VLS_93));
__VLS_95.slots.default;
const __VLS_96 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
    modelValue: (__VLS_ctx.form.guestName),
}));
const __VLS_98 = __VLS_97({
    modelValue: (__VLS_ctx.form.guestName),
}, ...__VLS_functionalComponentArgsRest(__VLS_97));
var __VLS_95;
const __VLS_100 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
    label: "电话",
}));
const __VLS_102 = __VLS_101({
    label: "电话",
}, ...__VLS_functionalComponentArgsRest(__VLS_101));
__VLS_103.slots.default;
const __VLS_104 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
    modelValue: (__VLS_ctx.form.guestPhone),
}));
const __VLS_106 = __VLS_105({
    modelValue: (__VLS_ctx.form.guestPhone),
}, ...__VLS_functionalComponentArgsRest(__VLS_105));
var __VLS_103;
const __VLS_108 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
    label: "入住",
}));
const __VLS_110 = __VLS_109({
    label: "入住",
}, ...__VLS_functionalComponentArgsRest(__VLS_109));
__VLS_111.slots.default;
const __VLS_112 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_113 = __VLS_asFunctionalComponent(__VLS_112, new __VLS_112({
    modelValue: (__VLS_ctx.form.checkInDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
}));
const __VLS_114 = __VLS_113({
    modelValue: (__VLS_ctx.form.checkInDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
}, ...__VLS_functionalComponentArgsRest(__VLS_113));
var __VLS_111;
const __VLS_116 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_117 = __VLS_asFunctionalComponent(__VLS_116, new __VLS_116({
    label: "离店",
}));
const __VLS_118 = __VLS_117({
    label: "离店",
}, ...__VLS_functionalComponentArgsRest(__VLS_117));
__VLS_119.slots.default;
const __VLS_120 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_121 = __VLS_asFunctionalComponent(__VLS_120, new __VLS_120({
    modelValue: (__VLS_ctx.form.checkOutDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
}));
const __VLS_122 = __VLS_121({
    modelValue: (__VLS_ctx.form.checkOutDate),
    type: "date",
    valueFormat: "YYYY-MM-DD",
}, ...__VLS_functionalComponentArgsRest(__VLS_121));
var __VLS_119;
const __VLS_124 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_125 = __VLS_asFunctionalComponent(__VLS_124, new __VLS_124({
    label: "房型",
}));
const __VLS_126 = __VLS_125({
    label: "房型",
}, ...__VLS_functionalComponentArgsRest(__VLS_125));
__VLS_127.slots.default;
const __VLS_128 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_129 = __VLS_asFunctionalComponent(__VLS_128, new __VLS_128({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.form.roomTypeId),
    placeholder: "选择房型",
}));
const __VLS_130 = __VLS_129({
    ...{ 'onChange': {} },
    modelValue: (__VLS_ctx.form.roomTypeId),
    placeholder: "选择房型",
}, ...__VLS_functionalComponentArgsRest(__VLS_129));
let __VLS_132;
let __VLS_133;
let __VLS_134;
const __VLS_135 = {
    onChange: (__VLS_ctx.onTypeChange)
};
__VLS_131.slots.default;
for (const [t] of __VLS_getVForSourceType((__VLS_ctx.roomTypes))) {
    const __VLS_136 = {}.ElOption;
    /** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
    // @ts-ignore
    const __VLS_137 = __VLS_asFunctionalComponent(__VLS_136, new __VLS_136({
        key: (t.id),
        label: (t.name),
        value: (t.id),
    }));
    const __VLS_138 = __VLS_137({
        key: (t.id),
        label: (t.name),
        value: (t.id),
    }, ...__VLS_functionalComponentArgsRest(__VLS_137));
}
var __VLS_131;
var __VLS_127;
const __VLS_140 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_141 = __VLS_asFunctionalComponent(__VLS_140, new __VLS_140({
    label: "房价",
}));
const __VLS_142 = __VLS_141({
    label: "房价",
}, ...__VLS_functionalComponentArgsRest(__VLS_141));
__VLS_143.slots.default;
const __VLS_144 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_145 = __VLS_asFunctionalComponent(__VLS_144, new __VLS_144({
    modelValue: (__VLS_ctx.form.rate),
    min: (0),
    precision: (2),
}));
const __VLS_146 = __VLS_145({
    modelValue: (__VLS_ctx.form.rate),
    min: (0),
    precision: (2),
}, ...__VLS_functionalComponentArgsRest(__VLS_145));
var __VLS_143;
const __VLS_148 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_149 = __VLS_asFunctionalComponent(__VLS_148, new __VLS_148({
    label: "备注",
}));
const __VLS_150 = __VLS_149({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_149));
__VLS_151.slots.default;
const __VLS_152 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_153 = __VLS_asFunctionalComponent(__VLS_152, new __VLS_152({
    modelValue: (__VLS_ctx.form.remark),
}));
const __VLS_154 = __VLS_153({
    modelValue: (__VLS_ctx.form.remark),
}, ...__VLS_functionalComponentArgsRest(__VLS_153));
var __VLS_151;
var __VLS_91;
{
    const { footer: __VLS_thisSlot } = __VLS_87.slots;
    const __VLS_156 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_157 = __VLS_asFunctionalComponent(__VLS_156, new __VLS_156({
        ...{ 'onClick': {} },
    }));
    const __VLS_158 = __VLS_157({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_157));
    let __VLS_160;
    let __VLS_161;
    let __VLS_162;
    const __VLS_163 = {
        onClick: (...[$event]) => {
            __VLS_ctx.dialogVisible = false;
        }
    };
    __VLS_159.slots.default;
    var __VLS_159;
    const __VLS_164 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_165 = __VLS_asFunctionalComponent(__VLS_164, new __VLS_164({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_166 = __VLS_165({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_165));
    let __VLS_168;
    let __VLS_169;
    let __VLS_170;
    const __VLS_171 = {
        onClick: (__VLS_ctx.create)
    };
    __VLS_167.slots.default;
    var __VLS_167;
}
var __VLS_87;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            list: list,
            page: page,
            pageSize: pageSize,
            total: total,
            roomTypes: roomTypes,
            dialogVisible: dialogVisible,
            form: form,
            load: load,
            openCreate: openCreate,
            onTypeChange: onTypeChange,
            create: create,
            confirm: confirm,
            cancel: cancel,
            checkin: checkin,
            statusText: statusText,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
