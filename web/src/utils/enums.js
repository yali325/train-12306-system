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
}

export const PASSENGER_TYPE_ARRAY = Object.values(PASSENGER_TYPE)
export const TRAIN_TYPE_ARRAY = Object.values(TRAIN_TYPE)
export const SEAT_TYPE_ARRAY = Object.values(SEAT_TYPE)
export const SEAT_COL_ARRAY = Object.values(SEAT_COL)
