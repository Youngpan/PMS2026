import { onMounted, onUnmounted, ref } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { housekeepingApi } from '@/api/modules';
const floors = ref([]);
async function load() { floors.value = await housekeepingApi.dashboard(); }
function onRoom(r) {
    ElMessageBox.confirm(`房号 ${r.number}（${statusText(r.status)}）`, '房务操作', {
        confirmButtonText: '报修', cancelButtonText: '派清扫', distinguishCancelAndClose: true,
    }).then(async () => {
        await housekeepingApi.reportMaintenance({ room_id: r.id, issue: '前台报修' });
        ElMessage.success('已报修');
        load();
    }).catch(async (action) => {
        if (action === 'cancel') {
            await housekeepingApi.createTask({ room_id: r.id, type: 'CHECKOUT_CLEAN', assignee_id: null });
            ElMessage.success('已派清扫');
            load();
        }
    });
}
function statusText(s) {
    return { VC: '净房', VD: '脏房', OCC: '在住', OOO: '维修', BLOCK: '预留' }[s] || s;
}
// 房态实时推送
let ws = null;
let reconnectTimer = null;
function connectWs() {
    const proto = location.protocol === 'https:' ? 'wss' : 'ws';
    ws = new WebSocket(`${proto}://${location.host}/api/v1/ws/room-status`);
    ws.onmessage = (ev) => {
        try {
            const msg = JSON.parse(ev.data);
            if (msg.type === 'room-status')
                load();
        }
        catch { }
    };
    ws.onclose = () => {
        reconnectTimer = setTimeout(connectWs, 3000);
    };
}
onMounted(() => { load(); connectWs(); });
onUnmounted(() => {
    if (reconnectTimer)
        clearTimeout(reconnectTimer);
    if (ws)
        ws.close();
});
debugger; /* PartiallyEnd: #3632/scriptSetup.vue */
const __VLS_ctx = {};
let __VLS_components;
let __VLS_directives;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
// CSS variable injection 
// CSS variable injection end 
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
for (const [floor] of __VLS_getVForSourceType((__VLS_ctx.floors))) {
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        key: (floor.floor),
        ...{ style: {} },
    });
    __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
        ...{ style: {} },
    });
    (floor.floor);
    const __VLS_13 = {}.ElRow;
    /** @type {[typeof __VLS_components.ElRow, typeof __VLS_components.elRow, typeof __VLS_components.ElRow, typeof __VLS_components.elRow, ]} */ ;
    // @ts-ignore
    const __VLS_14 = __VLS_asFunctionalComponent(__VLS_13, new __VLS_13({
        gutter: (8),
    }));
    const __VLS_15 = __VLS_14({
        gutter: (8),
    }, ...__VLS_functionalComponentArgsRest(__VLS_14));
    __VLS_16.slots.default;
    for (const [r] of __VLS_getVForSourceType((floor.rooms))) {
        const __VLS_17 = {}.ElCol;
        /** @type {[typeof __VLS_components.ElCol, typeof __VLS_components.elCol, typeof __VLS_components.ElCol, typeof __VLS_components.elCol, ]} */ ;
        // @ts-ignore
        const __VLS_18 = __VLS_asFunctionalComponent(__VLS_17, new __VLS_17({
            key: (r.id),
            span: (3),
        }));
        const __VLS_19 = __VLS_18({
            key: (r.id),
            span: (3),
        }, ...__VLS_functionalComponentArgsRest(__VLS_18));
        __VLS_20.slots.default;
        const __VLS_21 = {}.ElTooltip;
        /** @type {[typeof __VLS_components.ElTooltip, typeof __VLS_components.elTooltip, typeof __VLS_components.ElTooltip, typeof __VLS_components.elTooltip, ]} */ ;
        // @ts-ignore
        const __VLS_22 = __VLS_asFunctionalComponent(__VLS_21, new __VLS_21({
            content: (`房号 ${r.number} · ${__VLS_ctx.statusText(r.status)}`),
        }));
        const __VLS_23 = __VLS_22({
            content: (`房号 ${r.number} · ${__VLS_ctx.statusText(r.status)}`),
        }, ...__VLS_functionalComponentArgsRest(__VLS_22));
        __VLS_24.slots.default;
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ onClick: (...[$event]) => {
                    __VLS_ctx.onRoom(r);
                } },
            ...{ class: (['room', r.status]) },
        });
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (r.number);
        __VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
            ...{ style: {} },
        });
        (__VLS_ctx.statusText(r.status));
        var __VLS_24;
        var __VLS_20;
    }
    var __VLS_16;
}
__VLS_asFunctionalElement(__VLS_intrinsicElements.div, __VLS_intrinsicElements.div)({
    ...{ class: "legend" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "room VC small" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "room VD small" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "room OCC small" },
});
__VLS_asFunctionalElement(__VLS_intrinsicElements.span, __VLS_intrinsicElements.span)({
    ...{ class: "room OOO small" },
});
var __VLS_3;
/** @type {__VLS_StyleScopedClasses['legend']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['VC']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['VD']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['OCC']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
/** @type {__VLS_StyleScopedClasses['room']} */ ;
/** @type {__VLS_StyleScopedClasses['OOO']} */ ;
/** @type {__VLS_StyleScopedClasses['small']} */ ;
var __VLS_dollars;
const __VLS_self = (await import('vue')).defineComponent({
    setup() {
        return {
            floors: floors,
            load: load,
            onRoom: onRoom,
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
