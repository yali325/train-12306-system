export const PASSENGER_TYPE = {
  ADULT: { code: '1', desc: '成人' },
  CHILD: { code: '2', desc: '儿童' },
  STUDENT: { code: '3', desc: '学生' },
}

export const TRAIN_TYPE = {
  G: { code: 'G', desc: '高铁', priceRate: '1.2' },
  D: { code: 'D', desc: '动车', priceRate: '1' },
  K: { code: 'K', desc: '快速', priceRate: '0.8' },
}

export const SEAT_TYPE = {
  YDZ: { code: '1', desc: '一等座', price: '0.4' },
  EDZ: { code: '2', desc: '二等座', price: '0.3' },
  RW: { code: '3', desc: '软卧', price: '0.6' },
  YW: { code: '4', desc: '硬卧', price: '0.5' },
}

export const SEAT_COL = {
  YDZ_A: { code: 'A', desc: 'A', type: '1' },
  YDZ_C: { code: 'C', desc: 'C', type: '1' },
  YDZ_D: { code: 'D', desc: 'D', type: '1' },
  YDZ_F: { code: 'F', desc: 'F', type: '1' },
  EDZ_A: { code: 'A', desc: 'A', type: '2' },
  EDZ_B: { code: 'B', desc: 'B', type: '2' },
  EDZ_C: { code: 'C', desc: 'C', type: '2' },
  EDZ_D: { code: 'D', desc: 'D', type: '2' },
  EDZ_F: { code: 'F', desc: 'F', type: '2' },
  RW_A: { code: 'A', desc: '上铺A', type: '3' },
  RW_B: { code: 'B', desc: '下铺A', type: '3' },
  RW_C: { code: 'C', desc: '上铺B', type: '3' },
  RW_D: { code: 'D', desc: '下铺B', type: '3' },
  YW_A: { code: 'A', desc: '上铺A', type: '4' },
  YW_B: { code: 'B', desc: '中铺A', type: '4' },
  YW_C: { code: 'C', desc: '下铺A', type: '4' },
  YW_D: { code: 'D', desc: '上铺B', type: '4' },
  YW_E: { code: 'E', desc: '中铺B', type: '4' },
  YW_F: { code: 'F', desc: '下铺B', type: '4' },
}

export const CONFIRM_ORDER_STATUS = {
  INIT: { code: 'I', desc: '初始' },
  PENDING: { code: 'P', desc: '处理中' },
  SUCCESS: { code: 'S', desc: '成功' },
  FAILURE: { code: 'F', desc: '失败' },
  EMPTY: { code: 'E', desc: '无票' },
  CANCEL: { code: 'C', desc: '取消' },
}

export const PASSENGER_TYPE_ARRAY = Object.values(PASSENGER_TYPE)
export const TRAIN_TYPE_ARRAY = Object.values(TRAIN_TYPE)
export const SEAT_TYPE_ARRAY = Object.values(SEAT_TYPE)
export const SEAT_COL_ARRAY = Object.values(SEAT_COL)
export const CONFIRM_ORDER_STATUS_ARRAY = Object.values(CONFIRM_ORDER_STATUS)

Object.assign(window, {
  PASSENGER_TYPE,
  TRAIN_TYPE,
  SEAT_TYPE,
  SEAT_COL,
  CONFIRM_ORDER_STATUS,
  PASSENGER_TYPE_ARRAY,
  TRAIN_TYPE_ARRAY,
  SEAT_TYPE_ARRAY,
  SEAT_COL_ARRAY,
  CONFIRM_ORDER_STATUS_ARRAY,
})
