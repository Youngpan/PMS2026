import { nextTick, ref } from 'vue';
import * as echarts from 'echarts';
import { reportApi } from '@/api/modules';
const range = ref([]);
const revenue = ref(null);
const occ = ref(null);
const balances = ref(null);
const chartEl = ref();
let chart = null;
async function loadRevenue() {
    if (!range.value?.length)
        return;
    const [from, to] = range.value;
    revenue.value = await reportApi.revenue(from, to);
    const daily = await reportApi.revenueDaily(from, to);
    renderChart(daily);
}
async function loadOccupancy() {
    if (!range.value?.length)
        return;
    occ.value = await reportApi.occupancy(range.value[0], range.value[1]);
}
async function loadBalance() {
    balances.value = await reportApi.folioBalance();
}
function renderChart(daily) {
    nextTick(() => {
        if (!chartEl.value)
            return;
        chart = chart || echarts.init(chartEl.value);
        chart.setOption({
            tooltip: { trigger: 'axis' },
            grid: { left: 50, right: 20, top: 30, bottom: 30 },
            xAxis: { type: 'category', data: daily.map(d => d.date), axisLabel: { rotate: 30 } },
            yAxis: { type: 'value', name: '元' },
            series: [{
                    name: '每日营收', type: 'line', smooth: true, areaStyle: {},
                    data: daily.map(d => Number(d.amount)),
                    itemStyle: { color: '#409eff' },
                }],
        });
    });
}
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({});
const __VLS_0 = {}.ElCard;
/** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
// @ts-ignore
const __VLS_1 = __VLS_asFunctionalComponent(__VLS_0, new __VLS_0({
    ...{ style: {} },
}));
const __VLS_2 = __VLS_1({
    ...{ style: {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_1));
__VLS_3.slots.default;
{
    const { header: __VLS_thisSlot } = __VLS_3.slots;
}
const __VLS_4 = {}.ElForm;
/** @type {[typeof __VLS_components.ElForm, typeof __VLS_components.elForm, typeof __VLS_components.ElForm, typeof __VLS_components.elForm, ]} */ ;
// @ts-ignore
const __VLS_5 = __VLS_asFunctionalComponent(__VLS_4, new __VLS_4({
    inline: true,
}));
const __VLS_6 = __VLS_5({
    inline: true,
}, ...__VLS_functionalComponentArgsRest(__VLS_5));
__VLS_7.slots.default;
const __VLS_8 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_9 = __VLS_asFunctionalComponent(__VLS_8, new __VLS_8({
    label: "区间",
}));
const __VLS_10 = __VLS_9({
    label: "区间",
}, ...__VLS_functionalComponentArgsRest(__VLS_9));
__VLS_11.slots.default;
const __VLS_12 = {}.ElDatePicker;
/** @type {[typeof __VLS_components.ElDatePicker, typeof __VLS_components.elDatePicker, ]} */ ;
// @ts-ignore
const __VLS_13 = __VLS_asFunctionalComponent(__VLS_12, new __VLS_12({
    modelValue: (__VLS_ctx.range),
    type: "daterange",
    valueFormat: "YYYY-MM-DD",
}));
const __VLS_14 = __VLS_13({
    modelValue: (__VLS_ctx.range),
    type: "daterange",
    valueFormat: "YYYY-MM-DD",
}, ...__VLS_functionalComponentArgsRest(__VLS_13));
var __VLS_11;
const __VLS_16 = {}.ElFormItem;
/** @type {[typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, typeof __VLS_components.ElFormItem, typeof __VLS_components.elFormItem, ]} */ ;
// @ts-ignore
const __VLS_17 = __VLS_asFunctionalComponent(__VLS_16, new __VLS_16({}));
const __VLS_18 = __VLS_17({}, ...__VLS_functionalComponentArgsRest(__VLS_17));
__VLS_19.slots.default;
const __VLS_20 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_21 = __VLS_asFunctionalComponent(__VLS_20, new __VLS_20({
    ...{ 'onClick': {} },
    type: "primary",
}));
const __VLS_22 = __VLS_21({
    ...{ 'onClick': {} },
    type: "primary",
}, ...__VLS_functionalComponentArgsRest(__VLS_21));
let __VLS_24;
let __VLS_25;
let __VLS_26;
const __VLS_27 = {
    onClick: (__VLS_ctx.loadRevenue)
};
__VLS_23.slots.default;
var __VLS_23;
const __VLS_28 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_29 = __VLS_asFunctionalComponent(__VLS_28, new __VLS_28({
    ...{ 'onClick': {} },
    disabled: (!__VLS_ctx.range?.length),
}));
const __VLS_30 = __VLS_29({
    ...{ 'onClick': {} },
    disabled: (!__VLS_ctx.range?.length),
}, ...__VLS_functionalComponentArgsRest(__VLS_29));
let __VLS_32;
let __VLS_33;
let __VLS_34;
const __VLS_35 = {
    onClick: (__VLS_ctx.loadOccupancy)
};
__VLS_31.slots.default;
var __VLS_31;
const __VLS_36 = {}.ElButton;
/** @type {[typeof __VLS_components.ElButton, typeof __VLS_components.elButton, typeof __VLS_components.ElButton, typeof __VLS_components.elButton, ]} */ ;
// @ts-ignore
const __VLS_37 = __VLS_asFunctionalComponent(__VLS_36, new __VLS_36({
    ...{ 'onClick': {} },
}));
const __VLS_38 = __VLS_37({
    ...{ 'onClick': {} },
}, ...__VLS_functionalComponentArgsRest(__VLS_37));
let __VLS_40;
let __VLS_41;
let __VLS_42;
const __VLS_43 = {
    onClick: (__VLS_ctx.loadBalance)
};
__VLS_39.slots.default;
var __VLS_39;
var __VLS_19;
var __VLS_7;
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ref: "chartEl",
    ...{ style: {} },
});
/** @type {typeof __VLS_ctx.chartEl} */ ;
var __VLS_3;
if (__VLS_ctx.revenue) {
    const __VLS_44 = {}.ElCard;
    /** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
    // @ts-ignore
    const __VLS_45 = __VLS_asFunctionalComponent(__VLS_44, new __VLS_44({
        ...{ style: {} },
    }));
    const __VLS_46 = __VLS_45({
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_45));
    __VLS_47.slots.default;
    {
        const { header: __VLS_thisSlot } = __VLS_47.slots;
    }
    const __VLS_48 = {}.ElStatistic;
    /** @type {[typeof __VLS_components.ElStatistic, typeof __VLS_components.elStatistic, ]} */ ;
    // @ts-ignore
    const __VLS_49 = __VLS_asFunctionalComponent(__VLS_48, new __VLS_48({
        title: "营收合计",
        value: (__VLS_ctx.revenue.total),
        prefix: "¥",
    }));
    const __VLS_50 = __VLS_49({
        title: "营收合计",
        value: (__VLS_ctx.revenue.total),
        prefix: "¥",
    }, ...__VLS_functionalComponentArgsRest(__VLS_49));
    const __VLS_52 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_53 = __VLS_asFunctionalComponent(__VLS_52, new __VLS_52({
        data: (__VLS_ctx.revenue.byMethod),
        border: true,
        ...{ style: {} },
    }));
    const __VLS_54 = __VLS_53({
        data: (__VLS_ctx.revenue.byMethod),
        border: true,
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_53));
    __VLS_55.slots.default;
    const __VLS_56 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_57 = __VLS_asFunctionalComponent(__VLS_56, new __VLS_56({
        prop: "method",
        label: "支付方式",
    }));
    const __VLS_58 = __VLS_57({
        prop: "method",
        label: "支付方式",
    }, ...__VLS_functionalComponentArgsRest(__VLS_57));
    const __VLS_60 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_61 = __VLS_asFunctionalComponent(__VLS_60, new __VLS_60({
        prop: "amount",
        label: "金额",
    }));
    const __VLS_62 = __VLS_61({
        prop: "amount",
        label: "金额",
    }, ...__VLS_functionalComponentArgsRest(__VLS_61));
    var __VLS_55;
    var __VLS_47;
}
if (__VLS_ctx.occ) {
    const __VLS_64 = {}.ElCard;
    /** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
    // @ts-ignore
    const __VLS_65 = __VLS_asFunctionalComponent(__VLS_64, new __VLS_64({
        ...{ style: {} },
    }));
    const __VLS_66 = __VLS_65({
        ...{ style: {} },
    }, ...__VLS_functionalComponentArgsRest(__VLS_65));
    __VLS_67.slots.default;
    {
        const { header: __VLS_thisSlot } = __VLS_67.slots;
    }
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    const __VLS_68 = {}.ElStatistic;
    /** @type {[typeof __VLS_components.ElStatistic, typeof __VLS_components.elStatistic, ]} */ ;
    // @ts-ignore
    const __VLS_69 = __VLS_asFunctionalComponent(__VLS_68, new __VLS_68({
        title: "出租率",
        value: (__VLS_ctx.occ.occupancy * 100),
        precision: (1),
        suffix: "%",
    }));
    const __VLS_70 = __VLS_69({
        title: "出租率",
        value: (__VLS_ctx.occ.occupancy * 100),
        precision: (1),
        suffix: "%",
    }, ...__VLS_functionalComponentArgsRest(__VLS_69));
    const __VLS_72 = {}.ElStatistic;
    /** @type {[typeof __VLS_components.ElStatistic, typeof __VLS_components.elStatistic, ]} */ ;
    // @ts-ignore
    const __VLS_73 = __VLS_asFunctionalComponent(__VLS_72, new __VLS_72({
        title: "ADR",
        value: (__VLS_ctx.occ.adr),
        prefix: "¥",
    }));
    const __VLS_74 = __VLS_73({
        title: "ADR",
        value: (__VLS_ctx.occ.adr),
        prefix: "¥",
    }, ...__VLS_functionalComponentArgsRest(__VLS_73));
    const __VLS_76 = {}.ElStatistic;
    /** @type {[typeof __VLS_components.ElStatistic, typeof __VLS_components.elStatistic, ]} */ ;
    // @ts-ignore
    const __VLS_77 = __VLS_asFunctionalComponent(__VLS_76, new __VLS_76({
        title: "RevPAR",
        value: (__VLS_ctx.occ.revpar),
        prefix: "¥",
    }));
    const __VLS_78 = __VLS_77({
        title: "RevPAR",
        value: (__VLS_ctx.occ.revpar),
        prefix: "¥",
    }, ...__VLS_functionalComponentArgsRest(__VLS_77));
    const __VLS_80 = {}.ElStatistic;
    /** @type {[typeof __VLS_components.ElStatistic, typeof __VLS_components.elStatistic, ]} */ ;
    // @ts-ignore
    const __VLS_81 = __VLS_asFunctionalComponent(__VLS_80, new __VLS_80({
        title: "已售间夜",
        value: (__VLS_ctx.occ.soldRoomNights),
    }));
    const __VLS_82 = __VLS_81({
        title: "已售间夜",
        value: (__VLS_ctx.occ.soldRoomNights),
    }, ...__VLS_functionalComponentArgsRest(__VLS_81));
    var __VLS_67;
}
if (__VLS_ctx.balances) {
    const __VLS_84 = {}.ElCard;
    /** @type {[typeof __VLS_components.ElCard, typeof __VLS_components.elCard, typeof __VLS_components.ElCard, typeof __VLS_components.elCard, ]} */ ;
    // @ts-ignore
    const __VLS_85 = __VLS_asFunctionalComponent(__VLS_84, new __VLS_84({}));
    const __VLS_86 = __VLS_85({}, ...__VLS_functionalComponentArgsRest(__VLS_85));
    __VLS_87.slots.default;
    {
        const { header: __VLS_thisSlot } = __VLS_87.slots;
    }
    const __VLS_88 = {}.ElTable;
    /** @type {[typeof __VLS_components.ElTable, typeof __VLS_components.elTable, typeof __VLS_components.ElTable, typeof __VLS_components.elTable, ]} */ ;
    // @ts-ignore
    const __VLS_89 = __VLS_asFunctionalComponent(__VLS_88, new __VLS_88({
        data: (__VLS_ctx.balances),
        border: true,
    }));
    const __VLS_90 = __VLS_89({
        data: (__VLS_ctx.balances),
        border: true,
    }, ...__VLS_functionalComponentArgsRest(__VLS_89));
    __VLS_91.slots.default;
    const __VLS_92 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_93 = __VLS_asFunctionalComponent(__VLS_92, new __VLS_92({
        prop: "id",
        label: "账单ID",
        width: "100",
    }));
    const __VLS_94 = __VLS_93({
        prop: "id",
        label: "账单ID",
        width: "100",
    }, ...__VLS_functionalComponentArgsRest(__VLS_93));
    const __VLS_96 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_97 = __VLS_asFunctionalComponent(__VLS_96, new __VLS_96({
        prop: "stayId",
        label: "在住ID",
        width: "100",
    }));
    const __VLS_98 = __VLS_97({
        prop: "stayId",
        label: "在住ID",
        width: "100",
    }, ...__VLS_functionalComponentArgsRest(__VLS_97));
    const __VLS_100 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_101 = __VLS_asFunctionalComponent(__VLS_100, new __VLS_100({
        prop: "guestId",
        label: "宾客ID",
        width: "100",
    }));
    const __VLS_102 = __VLS_101({
        prop: "guestId",
        label: "宾客ID",
        width: "100",
    }, ...__VLS_functionalComponentArgsRest(__VLS_101));
    const __VLS_104 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_105 = __VLS_asFunctionalComponent(__VLS_104, new __VLS_104({
        prop: "balance",
        label: "余额",
    }));
    const __VLS_106 = __VLS_105({
        prop: "balance",
        label: "余额",
    }, ...__VLS_functionalComponentArgsRest(__VLS_105));
    const __VLS_108 = {}.ElTableColumn;
    /** @type {[typeof __VLS_components.ElTableColumn, typeof __VLS_components.elTableColumn, ]} */ ;
    // @ts-ignore
    const __VLS_109 = __VLS_asFunctionalComponent(__VLS_108, new __VLS_108({
        prop: "status",
        label: "状态",
    }));
    const __VLS_110 = __VLS_109({
        prop: "status",
        label: "状态",
    }, ...__VLS_functionalComponentArgsRest(__VLS_109));
    var __VLS_91;
    var __VLS_87;
}
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            range: range,
            revenue: revenue,
            occ: occ,
            balances: balances,
            chartEl: chartEl,
            loadRevenue: loadRevenue,
            loadOccupancy: loadOccupancy,
            loadBalance: loadBalance,
        };
    },
});
export default (await import('vue')).defineComponent({
    setup() {
        return {};
    },
});
; /* PartiallyEnd: #4569/main.vue */
