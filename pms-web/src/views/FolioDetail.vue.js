import { onMounted, reactive, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { folioApi } from '@/api/modules';
const route = useRoute();
const id = Number(route.params.id);
const folio = ref({ folio: {}, items: [] });
const dialog = reactive({ charge: false, deposit: false, pay: false });
const form = reactive({
    charge: { category: '杂费', amount: 0, remark: '' },
    deposit: { amount: 0, method: 'CASH' },
    pay: { amount: 0, method: 'WECHAT' },
});
async function load() { folio.value = await folioApi.detail(id); }
async function addCharge() { await folioApi.addCharge(id, form.charge); dialog.charge = false; load(); }
async function addDeposit() { await folioApi.addDeposit(id, form.deposit); dialog.deposit = false; load(); }
async function pay() { await folioApi.pay(id, form.pay.method, form.pay.amount); dialog.pay = false; load(); }
async function receipt() {
    const r = await folioApi.receipt(id);
    ElMessageBox.alert(r.text, '收据', { confirmButtonText: '关闭' });
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
    (__VLS_ctx.folio.folio?.id);
    (__VLS_ctx.folio.folio?.balance);
    (__VLS_ctx.folio.folio?.status);
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
    data: (__VLS_ctx.folio.items),
    border: true,
    stripe: true,
}));
const __VLS_15 = __VLS_14({
    data: (__VLS_ctx.folio.items),
    border: true,
    stripe: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_14));
__VLS_16.slots.default;
const __VLS_17 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
    prop: "type",
    label: "类型",
    width: "100",
}));
const __VLS_19 = __VLS_18({
    prop: "type",
    label: "类型",
    width: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_18));
const __VLS_21 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_22 = __VLS_asFunctionalComponent(__VLS_21, new __VLS_21({
    prop: "category",
    label: "项目",
    width: "120",
}));
const __VLS_23 = __VLS_22({
    prop: "category",
    label: "项目",
    width: "120",
}, ...__VLS_functionalComponentArgsRest(__VLS_22));
const __VLS_25 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_26 = __VLS_asFunctionalComponent(__VLS_25, new __VLS_25({
    prop: "amount",
    label: "金额",
    width: "100",
}));
const __VLS_27 = __VLS_26({
    prop: "amount",
    label: "金额",
    width: "100",
}, ...__VLS_functionalComponentArgsRest(__VLS_26));
const __VLS_29 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_30 = __VLS_asFunctionalComponent(__VLS_29, new __VLS_29({
    prop: "remark",
    label: "备注",
}));
const __VLS_31 = __VLS_30({
    prop: "remark",
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_30));
const __VLS_33 = {}.ElTableColumn;
/** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
// @ts-ignore
const __VLS_34 = __VLS_asFunctionalComponent(__VLS_33, new __VLS_33({
    prop: "createdAt",
    label: "时间",
    width: "180",
}));
const __VLS_35 = __VLS_34({
    prop: "createdAt",
    label: "时间",
    width: "180",
}, ...__VLS_functionalComponentArgsRest(__VLS_34));
var __VLS_16;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ style: {} },
});
const __VLS_37 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_38 = __VLS_asFunctionalComponent(__VLS_37, new __VLS_37({
    ...{ 'onClick': {} },
}));
const __VLS_39 = __VLS_38({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_38));
let __VLS_41;
let __VLS_42;
let __VLS_43;
const __VLS_44 = {
    onClick: (...[$event]) => {
        __VLS_ctx.dialog.charge = true;
    }
};
__VLS_40.slots.default;
var __VLS_40;
const __VLS_45 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_46 = __VLS_asFunctionalComponent(__VLS_45, new __VLS_45({
    ...{ 'onClick': {} },
}));
const __VLS_47 = __VLS_46({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_46));
let __VLS_49;
let __VLS_50;
let __VLS_51;
const __VLS_52 = {
    onClick: (...[$event]) => {
        __VLS_ctx.dialog.deposit = true;
    }
};
__VLS_48.slots.default;
var __VLS_48;
const __VLS_53 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_54 = __VLS_asFunctionalComponent(__VLS_53, new __VLS_53({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_55 = __VLS_54({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_54));
let __VLS_57;
let __VLS_58;
let __VLS_59;
const __VLS_60 = {
    onClick: (...[$event]) => {
        __VLS_ctx.dialog.pay = true;
    }
};
__VLS_56.slots.default;
var __VLS_56;
const __VLS_61 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_62 = __VLS_asFunctionalComponent(__VLS_61, new __VLS_61({
    ...{ 'onClick': {} },
}));
const __VLS_63 = __VLS_62({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_62));
let __VLS_65;
let __VLS_66;
let __VLS_67;
const __VLS_68 = {
    onClick: (__VLS_ctx.receipt)
};
__VLS_64.slots.default;
var __VLS_64;
const __VLS_69 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_70 = __VLS_asFunctionalComponent(__VLS_69, new __VLS_69({
    modelValue: (__VLS_ctx.dialog.charge),
    title: "录消费",
    width: "360px",
}));
const __VLS_71 = __VLS_70({
    modelValue: (__VLS_ctx.dialog.charge),
    title: "录消费",
    width: "360px",
}, ...__VLS_functionalComponentArgsRest(__VLS_70));
__VLS_72.slots.default;
const __VLS_73 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_74 = __VLS_asFunctionalComponent(__VLS_73, new __VLS_73({
    labelWidth: "80px",
}));
const __VLS_75 = __VLS_74({
    labelWidth: "80px",
}, ...__VLS_functionalComponentArgsRest(__VLS_74));
__VLS_76.slots.default;
const __VLS_77 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_78 = __VLS_asFunctionalComponent(__VLS_77, new __VLS_77({
    label: "项目",
}));
const __VLS_79 = __VLS_78({
    label: "项目",
}, ...__VLS_functionalComponentArgsRest(__VLS_78));
__VLS_80.slots.default;
const __VLS_81 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_82 = __VLS_asFunctionalComponent(__VLS_81, new __VLS_81({
    modelValue: (__VLS_ctx.form.charge.category),
}));
const __VLS_83 = __VLS_82({
    modelValue: (__VLS_ctx.form.charge.category),
}, ...__VLS_functionalComponentArgsRest(__VLS_82));
var __VLS_80;
const __VLS_85 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_86 = __VLS_asFunctionalComponent(__VLS_85, new __VLS_85({
    label: "金额",
}));
const __VLS_87 = __VLS_86({
    label: "金额",
}, ...__VLS_functionalComponentArgsRest(__VLS_86));
__VLS_88.slots.default;
const __VLS_89 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_90 = __VLS_asFunctionalComponent(__VLS_89, new __VLS_89({
    modelValue: (__VLS_ctx.form.charge.amount),
    min: (0),
    precision: (2),
}));
const __VLS_91 = __VLS_90({
    modelValue: (__VLS_ctx.form.charge.amount),
    min: (0),
    precision: (2),
}, ...__VLS_functionalComponentArgsRest(__VLS_90));
var __VLS_88;
const __VLS_93 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_94 = __VLS_asFunctionalComponent(__VLS_93, new __VLS_93({
    label: "备注",
}));
const __VLS_95 = __VLS_94({
    label: "备注",
}, ...__VLS_functionalComponentArgsRest(__VLS_94));
__VLS_96.slots.default;
const __VLS_97 = {}.ElInput;
/** @type {[typeof __VLS_components.ElInput, typeof __VLS_components.elInput, ]} */ ;
// @ts-ignore
const __VLS_98 = __VLS_asFunctionalComponent(__VLS_97, new __VLS_97({
    modelValue: (__VLS_ctx.form.charge.remark),
}));
const __VLS_99 = __VLS_98({
    modelValue: (__VLS_ctx.form.charge.remark),
}, ...__VLS_functionalComponentArgsRest(__VLS_98));
var __VLS_96;
var __VLS_76;
{
    const { footer: __VLS_thisSlot } = __VLS_72.slots;
    const __VLS_101 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_102 = __VLS_asFunctionalComponent(__VLS_101, new __VLS_101({
        ...{ 'onClick': {} },
    }));
    const __VLS_103 = __VLS_102({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_102));
    let __VLS_105;
    let __VLS_106;
    let __VLS_107;
    const __VLS_108 = {
        onClick: (...[$event]) => {
            __VLS_ctx.dialog.charge = false;
        }
    };
    __VLS_104.slots.default;
    var __VLS_104;
    const __VLS_109 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_110 = __VLS_asFunctionalComponent(__VLS_109, new __VLS_109({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_111 = __VLS_110({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_110));
    let __VLS_113;
    let __VLS_114;
    let __VLS_115;
    const __VLS_116 = {
        onClick: (__VLS_ctx.addCharge)
    };
    __VLS_112.slots.default;
    var __VLS_112;
}
var __VLS_72;
const __VLS_117 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_118 = __VLS_asFunctionalComponent(__VLS_117, new __VLS_117({
    modelValue: (__VLS_ctx.dialog.deposit),
    title: "录押金",
    width: "360px",
}));
const __VLS_119 = __VLS_118({
    modelValue: (__VLS_ctx.dialog.deposit),
    title: "录押金",
    width: "360px",
}, ...__VLS_functionalComponentArgsRest(__VLS_118));
__VLS_120.slots.default;
const __VLS_121 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_122 = __VLS_asFunctionalComponent(__VLS_121, new __VLS_121({
    labelWidth: "80px",
}));
const __VLS_123 = __VLS_122({
    labelWidth: "80px",
}, ...__VLS_functionalComponentArgsRest(__VLS_122));
__VLS_124.slots.default;
const __VLS_125 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_126 = __VLS_asFunctionalComponent(__VLS_125, new __VLS_125({
    label: "金额",
}));
const __VLS_127 = __VLS_126({
    label: "金额",
}, ...__VLS_functionalComponentArgsRest(__VLS_126));
__VLS_128.slots.default;
const __VLS_129 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_130 = __VLS_asFunctionalComponent(__VLS_129, new __VLS_129({
    modelValue: (__VLS_ctx.form.deposit.amount),
    min: (0),
    precision: (2),
}));
const __VLS_131 = __VLS_130({
    modelValue: (__VLS_ctx.form.deposit.amount),
    min: (0),
    precision: (2),
}, ...__VLS_functionalComponentArgsRest(__VLS_130));
var __VLS_128;
const __VLS_133 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_134 = __VLS_asFunctionalComponent(__VLS_133, new __VLS_133({
    label: "方式",
}));
const __VLS_135 = __VLS_134({
    label: "方式",
}, ...__VLS_functionalComponentArgsRest(__VLS_134));
__VLS_136.slots.default;
const __VLS_137 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_138 = __VLS_asFunctionalComponent(__VLS_137, new __VLS_137({
    modelValue: (__VLS_ctx.form.deposit.method),
}));
const __VLS_139 = __VLS_138({
    modelValue: (__VLS_ctx.form.deposit.method),
}, ...__VLS_functionalComponentArgsRest(__VLS_138));
__VLS_140.slots.default;
const __VLS_141 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_142 = __VLS_asFunctionalComponent(__VLS_141, new __VLS_141({
    label: "现金",
    value: "CASH",
}));
const __VLS_143 = __VLS_142({
    label: "现金",
    value: "CASH",
}, ...__VLS_functionalComponentArgsRest(__VLS_142));
const __VLS_145 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_146 = __VLS_asFunctionalComponent(__VLS_145, new __VLS_145({
    label: "微信",
    value: "WECHAT",
}));
const __VLS_147 = __VLS_146({
    label: "微信",
    value: "WECHAT",
}, ...__VLS_functionalComponentArgsRest(__VLS_146));
const __VLS_149 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_150 = __VLS_asFunctionalComponent(__VLS_149, new __VLS_149({
    label: "支付宝",
    value: "ALIPAY",
}));
const __VLS_151 = __VLS_150({
    label: "支付宝",
    value: "ALIPAY",
}, ...__VLS_functionalComponentArgsRest(__VLS_150));
var __VLS_140;
var __VLS_136;
var __VLS_124;
{
    const { footer: __VLS_thisSlot } = __VLS_120.slots;
    const __VLS_153 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_154 = __VLS_asFunctionalComponent(__VLS_153, new __VLS_153({
        ...{ 'onClick': {} },
    }));
    const __VLS_155 = __VLS_154({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_154));
    let __VLS_157;
    let __VLS_158;
    let __VLS_159;
    const __VLS_160 = {
        onClick: (...[$event]) => {
            __VLS_ctx.dialog.deposit = false;
        }
    };
    __VLS_156.slots.default;
    var __VLS_156;
    const __VLS_161 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_162 = __VLS_asFunctionalComponent(__VLS_161, new __VLS_161({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_163 = __VLS_162({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_162));
    let __VLS_165;
    let __VLS_166;
    let __VLS_167;
    const __VLS_168 = {
        onClick: (__VLS_ctx.addDeposit)
    };
    __VLS_164.slots.default;
    var __VLS_164;
}
var __VLS_120;
const __VLS_169 = {}.ElDialog;
/** @type {[typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, typeof __VLS_components.ElDialog, typeof __VLS_components.elDialog, ]} */ ;
// @ts-ignore
const __VLS_170 = __VLS_asFunctionalComponent(__VLS_169, new __VLS_169({
    modelValue: (__VLS_ctx.dialog.pay),
    title: "收款",
    width: "360px",
}));
const __VLS_171 = __VLS_170({
    modelValue: (__VLS_ctx.dialog.pay),
    title: "收款",
    width: "360px",
}, ...__VLS_functionalComponentArgsRest(__VLS_170));
__VLS_172.slots.default;
const __VLS_173 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_174 = __VLS_asFunctionalComponent(__VLS_173, new __VLS_173({
    labelWidth: "80px",
}));
const __VLS_175 = __VLS_174({
    labelWidth: "80px",
}, ...__VLS_functionalComponentArgsRest(__VLS_174));
__VLS_176.slots.default;
const __VLS_177 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_178 = __VLS_asFunctionalComponent(__VLS_177, new __VLS_177({
    label: "应收",
}));
const __VLS_179 = __VLS_178({
    label: "应收",
}, ...__VLS_functionalComponentArgsRest(__VLS_178));
__VLS_180.slots.default;
(__VLS_ctx.folio.folio?.balance);
var __VLS_180;
const __VLS_181 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_182 = __VLS_asFunctionalComponent(__VLS_181, new __VLS_181({
    label: "金额",
}));
const __VLS_183 = __VLS_182({
    label: "金额",
}, ...__VLS_functionalComponentArgsRest(__VLS_182));
__VLS_184.slots.default;
const __VLS_185 = {}.ElInputNumber;
/** @type {[typeof __VLS_components.ElInputNumber, typeof __VLS_components.elInputNumber, ]} */ ;
// @ts-ignore
const __VLS_186 = __VLS_asFunctionalComponent(__VLS_185, new __VLS_185({
    modelValue: (__VLS_ctx.form.pay.amount),
    min: (0),
    precision: (2),
}));
const __VLS_187 = __VLS_186({
    modelValue: (__VLS_ctx.form.pay.amount),
    min: (0),
    precision: (2),
}, ...__VLS_functionalComponentArgsRest(__VLS_186));
var __VLS_184;
const __VLS_189 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_190 = __VLS_asFunctionalComponent(__VLS_189, new __VLS_189({
    label: "方式",
}));
const __VLS_191 = __VLS_190({
    label: "方式",
}, ...__VLS_functionalComponentArgsRest(__VLS_190));
__VLS_192.slots.default;
const __VLS_193 = {}.ElSelect;
/** @type {[typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, typeof __VLS_components.ElSelect, typeof __VLS_components.elSelect, ]} */ ;
// @ts-ignore
const __VLS_194 = __VLS_asFunctionalComponent(__VLS_193, new __VLS_193({
    modelValue: (__VLS_ctx.form.pay.method),
}));
const __VLS_195 = __VLS_194({
    modelValue: (__VLS_ctx.form.pay.method),
}, ...__VLS_functionalComponentArgsRest(__VLS_194));
__VLS_196.slots.default;
const __VLS_197 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_198 = __VLS_asFunctionalComponent(__VLS_197, new __VLS_197({
    label: "现金",
    value: "CASH",
}));
const __VLS_199 = __VLS_198({
    label: "现金",
    value: "CASH",
}, ...__VLS_functionalComponentArgsRest(__VLS_198));
const __VLS_201 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_202 = __VLS_asFunctionalComponent(__VLS_201, new __VLS_201({
    label: "微信",
    value: "WECHAT",
}));
const __VLS_203 = __VLS_202({
    label: "微信",
    value: "WECHAT",
}, ...__VLS_functionalComponentArgsRest(__VLS_202));
const __VLS_205 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_206 = __VLS_asFunctionalComponent(__VLS_205, new __VLS_205({
    label: "支付宝",
    value: "ALIPAY",
}));
const __VLS_207 = __VLS_206({
    label: "支付宝",
    value: "ALIPAY",
}, ...__VLS_functionalComponentArgsRest(__VLS_206));
const __VLS_209 = {}.ElOption;
/** @type {[typeof __VLS_components.ElOption, typeof __VLS_components.elOption, ]} */ ;
// @ts-ignore
const __VLS_210 = __VLS_asFunctionalComponent(__VLS_209, new __VLS_209({
    label: "银行卡",
    value: "CARD",
}));
const __VLS_211 = __VLS_210({
    label: "银行卡",
    value: "CARD",
}, ...__VLS_functionalComponentArgsRest(__VLS_210));
var __VLS_196;
var __VLS_192;
var __VLS_176;
{
    const { footer: __VLS_thisSlot } = __VLS_172.slots;
    const __VLS_213 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_214 = __VLS_asFunctionalComponent(__VLS_213, new __VLS_213({
        ...{ 'onClick': {} },
    }));
    const __VLS_215 = __VLS_214({
        ...{ 'onClick': {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_214));
    let __VLS_217;
    let __VLS_218;
    let __VLS_219;
    const __VLS_220 = {
        onClick: (...[$event]) => {
            __VLS_ctx.dialog.pay = false;
        }
    };
    __VLS_216.slots.default;
    var __VLS_216;
    const __VLS_221 = {}.ElButton;
    /** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
    // @ts-ignore
    const __VLS_222 = __VLS_asFunctionalComponent(__VLS_221, new __VLS_221({
        ...{ 'onClick': {} },
        type: "primary",
    }));
    const __VLS_223 = __VLS_222({
        ...{ 'onClick': {} },
        type: "primary",
    }, ...__VLS_functionalComponentArgsRest(__VLS_222));
    let __VLS_225;
    let __VLS_226;
    let __VLS_227;
    const __VLS_228 = {
        onClick: (__VLS_ctx.pay)
    };
    __VLS_224.slots.default;
    var __VLS_224;
}
var __VLS_172;
var __VLS_3;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            folio: folio,
            dialog: dialog,
            form: form,
            load: load,
            addCharge: addCharge,
            addDeposit: addDeposit,
            pay: pay,
            receipt: receipt,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
