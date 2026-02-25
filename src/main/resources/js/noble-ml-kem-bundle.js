// Bundled @noble/post-quantum ML-KEM implementation for GraalVM
// This is the actual Noble library implementation for cross-platform compatibility
// Version: @noble/post-quantum@0.4.1

(function(global) {
    'use strict';

    // Polyfills for GraalVM if needed
    if (typeof crypto === 'undefined' && typeof Java !== 'undefined') {
        global.crypto = {
            getRandomValues: function(array) {
                const JavaSecureRandom = Java.type('java.security.SecureRandom');
                const random = new JavaSecureRandom();
                for (let i = 0; i < array.length; i++) {
                    array[i] = random.nextInt(256);
                }
                return array;
            }
        };
    }

    // Load the bundled Noble library
    var NobleBundle = (() => {
  // node_modules/@noble/hashes/esm/_u64.js
  var U32_MASK64 = /* @__PURE__ */ BigInt(2 ** 32 - 1);
  var _32n = /* @__PURE__ */ BigInt(32);
  function fromBig(n, le = false) {
    if (le)
      return { h: Number(n & U32_MASK64), l: Number(n >> _32n & U32_MASK64) };
    return { h: Number(n >> _32n & U32_MASK64) | 0, l: Number(n & U32_MASK64) | 0 };
  }
  function split(lst, le = false) {
    const len = lst.length;
    let Ah = new Uint32Array(len);
    let Al = new Uint32Array(len);
    for (let i = 0; i < len; i++) {
      const { h, l } = fromBig(lst[i], le);
      [Ah[i], Al[i]] = [h, l];
    }
    return [Ah, Al];
  }
  var shrSH = (h, _l, s) => h >>> s;
  var shrSL = (h, l, s) => h << 32 - s | l >>> s;
  var rotrSH = (h, l, s) => h >>> s | l << 32 - s;
  var rotrSL = (h, l, s) => h << 32 - s | l >>> s;
  var rotrBH = (h, l, s) => h << 64 - s | l >>> s - 32;
  var rotrBL = (h, l, s) => h >>> s - 32 | l << 64 - s;
  var rotlSH = (h, l, s) => h << s | l >>> 32 - s;
  var rotlSL = (h, l, s) => l << s | h >>> 32 - s;
  var rotlBH = (h, l, s) => l << s - 32 | h >>> 64 - s;
  var rotlBL = (h, l, s) => h << s - 32 | l >>> 64 - s;
  function add(Ah, Al, Bh, Bl) {
    const l = (Al >>> 0) + (Bl >>> 0);
    return { h: Ah + Bh + (l / 2 ** 32 | 0) | 0, l: l | 0 };
  }
  var add3L = (Al, Bl, Cl) => (Al >>> 0) + (Bl >>> 0) + (Cl >>> 0);
  var add3H = (low, Ah, Bh, Ch) => Ah + Bh + Ch + (low / 2 ** 32 | 0) | 0;
  var add4L = (Al, Bl, Cl, Dl) => (Al >>> 0) + (Bl >>> 0) + (Cl >>> 0) + (Dl >>> 0);
  var add4H = (low, Ah, Bh, Ch, Dh) => Ah + Bh + Ch + Dh + (low / 2 ** 32 | 0) | 0;
  var add5L = (Al, Bl, Cl, Dl, El) => (Al >>> 0) + (Bl >>> 0) + (Cl >>> 0) + (Dl >>> 0) + (El >>> 0);
  var add5H = (low, Ah, Bh, Ch, Dh, Eh) => Ah + Bh + Ch + Dh + Eh + (low / 2 ** 32 | 0) | 0;

  // node_modules/@noble/hashes/esm/crypto.js
  var crypto = typeof globalThis === "object" && "crypto" in globalThis ? globalThis.crypto : void 0;

  // node_modules/@noble/hashes/esm/utils.js
  function isBytes(a) {
    return a instanceof Uint8Array || ArrayBuffer.isView(a) && a.constructor.name === "Uint8Array";
  }
  function anumber(n) {
    if (!Number.isSafeInteger(n) || n < 0)
      throw new Error("positive integer expected, got " + n);
  }
  function abytes(b, ...lengths) {
    if (!isBytes(b))
      throw new Error("Uint8Array expected");
    if (lengths.length > 0 && !lengths.includes(b.length))
      throw new Error("Uint8Array expected of length " + lengths + ", got length=" + b.length);
  }
  function aexists(instance, checkFinished = true) {
    if (instance.destroyed)
      throw new Error("Hash instance has been destroyed");
    if (checkFinished && instance.finished)
      throw new Error("Hash#digest() has already been called");
  }
  function aoutput(out, instance) {
    abytes(out);
    const min = instance.outputLen;
    if (out.length < min) {
      throw new Error("digestInto() expects output buffer of length at least " + min);
    }
  }
  function u32(arr) {
    return new Uint32Array(arr.buffer, arr.byteOffset, Math.floor(arr.byteLength / 4));
  }
  function clean(...arrays) {
    for (let i = 0; i < arrays.length; i++) {
      arrays[i].fill(0);
    }
  }
  function createView(arr) {
    return new DataView(arr.buffer, arr.byteOffset, arr.byteLength);
  }
  function rotr(word, shift) {
    return word << 32 - shift | word >>> shift;
  }
  var isLE = /* @__PURE__ */ (() => new Uint8Array(new Uint32Array([287454020]).buffer)[0] === 68)();
  function byteSwap(word) {
    return word << 24 & 4278190080 | word << 8 & 16711680 | word >>> 8 & 65280 | word >>> 24 & 255;
  }
  function byteSwap32(arr) {
    for (let i = 0; i < arr.length; i++) {
      arr[i] = byteSwap(arr[i]);
    }
    return arr;
  }
  var swap32IfBE = isLE ? (u) => u : byteSwap32;
  var hasHexBuiltin = /* @__PURE__ */ (() => (
    // @ts-ignore
    typeof Uint8Array.from([]).toHex === "function" && typeof Uint8Array.fromHex === "function"
  ))();
  var asciis = { _0: 48, _9: 57, A: 65, F: 70, a: 97, f: 102 };
  function asciiToBase16(ch) {
    if (ch >= asciis._0 && ch <= asciis._9)
      return ch - asciis._0;
    if (ch >= asciis.A && ch <= asciis.F)
      return ch - (asciis.A - 10);
    if (ch >= asciis.a && ch <= asciis.f)
      return ch - (asciis.a - 10);
    return;
  }
  function hexToBytes(hex) {
    if (typeof hex !== "string")
      throw new Error("hex string expected, got " + typeof hex);
    if (hasHexBuiltin)
      return Uint8Array.fromHex(hex);
    const hl = hex.length;
    const al = hl / 2;
    if (hl % 2)
      throw new Error("hex string expected, got unpadded hex of length " + hl);
    const array = new Uint8Array(al);
    for (let ai = 0, hi = 0; ai < al; ai++, hi += 2) {
      const n1 = asciiToBase16(hex.charCodeAt(hi));
      const n2 = asciiToBase16(hex.charCodeAt(hi + 1));
      if (n1 === void 0 || n2 === void 0) {
        const char = hex[hi] + hex[hi + 1];
        throw new Error('hex string expected, got non-hex character "' + char + '" at index ' + hi);
      }
      array[ai] = n1 * 16 + n2;
    }
    return array;
  }
  function utf8ToBytes(str) {
    if (typeof str !== "string")
      throw new Error("string expected");
    return new Uint8Array(new TextEncoder().encode(str));
  }
  function toBytes(data) {
    if (typeof data === "string")
      data = utf8ToBytes(data);
    abytes(data);
    return data;
  }
  var Hash = class {
  };
  function createHasher(hashCons) {
    const hashC = (msg) => hashCons().update(toBytes(msg)).digest();
    const tmp = hashCons();
    hashC.outputLen = tmp.outputLen;
    hashC.blockLen = tmp.blockLen;
    hashC.create = () => hashCons();
    return hashC;
  }
  function createXOFer(hashCons) {
    const hashC = (msg, opts2) => hashCons(opts2).update(toBytes(msg)).digest();
    const tmp = hashCons({});
    hashC.outputLen = tmp.outputLen;
    hashC.blockLen = tmp.blockLen;
    hashC.create = (opts2) => hashCons(opts2);
    return hashC;
  }
  function randomBytes(bytesLength = 32) {
    if (crypto && typeof crypto.getRandomValues === "function") {
      return crypto.getRandomValues(new Uint8Array(bytesLength));
    }
    if (crypto && typeof crypto.randomBytes === "function") {
      return Uint8Array.from(crypto.randomBytes(bytesLength));
    }
    throw new Error("crypto.getRandomValues must be defined");
  }

  // node_modules/@noble/hashes/esm/sha3.js
  var _0n = BigInt(0);
  var _1n = BigInt(1);
  var _2n = BigInt(2);
  var _7n = BigInt(7);
  var _256n = BigInt(256);
  var _0x71n = BigInt(113);
  var SHA3_PI = [];
  var SHA3_ROTL = [];
  var _SHA3_IOTA = [];
  for (let round = 0, R = _1n, x = 1, y = 0; round < 24; round++) {
    [x, y] = [y, (2 * x + 3 * y) % 5];
    SHA3_PI.push(2 * (5 * y + x));
    SHA3_ROTL.push((round + 1) * (round + 2) / 2 % 64);
    let t = _0n;
    for (let j = 0; j < 7; j++) {
      R = (R << _1n ^ (R >> _7n) * _0x71n) % _256n;
      if (R & _2n)
        t ^= _1n << (_1n << /* @__PURE__ */ BigInt(j)) - _1n;
    }
    _SHA3_IOTA.push(t);
  }
  var IOTAS = split(_SHA3_IOTA, true);
  var SHA3_IOTA_H = IOTAS[0];
  var SHA3_IOTA_L = IOTAS[1];
  var rotlH = (h, l, s) => s > 32 ? rotlBH(h, l, s) : rotlSH(h, l, s);
  var rotlL = (h, l, s) => s > 32 ? rotlBL(h, l, s) : rotlSL(h, l, s);
  function keccakP(s, rounds = 24) {
    const B = new Uint32Array(5 * 2);
    for (let round = 24 - rounds; round < 24; round++) {
      for (let x = 0; x < 10; x++)
        B[x] = s[x] ^ s[x + 10] ^ s[x + 20] ^ s[x + 30] ^ s[x + 40];
      for (let x = 0; x < 10; x += 2) {
        const idx1 = (x + 8) % 10;
        const idx0 = (x + 2) % 10;
        const B0 = B[idx0];
        const B1 = B[idx0 + 1];
        const Th = rotlH(B0, B1, 1) ^ B[idx1];
        const Tl = rotlL(B0, B1, 1) ^ B[idx1 + 1];
        for (let y = 0; y < 50; y += 10) {
          s[x + y] ^= Th;
          s[x + y + 1] ^= Tl;
        }
      }
      let curH = s[2];
      let curL = s[3];
      for (let t = 0; t < 24; t++) {
        const shift = SHA3_ROTL[t];
        const Th = rotlH(curH, curL, shift);
        const Tl = rotlL(curH, curL, shift);
        const PI = SHA3_PI[t];
        curH = s[PI];
        curL = s[PI + 1];
        s[PI] = Th;
        s[PI + 1] = Tl;
      }
      for (let y = 0; y < 50; y += 10) {
        for (let x = 0; x < 10; x++)
          B[x] = s[y + x];
        for (let x = 0; x < 10; x++)
          s[y + x] ^= ~B[(x + 2) % 10] & B[(x + 4) % 10];
      }
      s[0] ^= SHA3_IOTA_H[round];
      s[1] ^= SHA3_IOTA_L[round];
    }
    clean(B);
  }
  var Keccak = class _Keccak extends Hash {
    // NOTE: we accept arguments in bytes instead of bits here.
    constructor(blockLen, suffix, outputLen, enableXOF = false, rounds = 24) {
      super();
      this.pos = 0;
      this.posOut = 0;
      this.finished = false;
      this.destroyed = false;
      this.enableXOF = false;
      this.blockLen = blockLen;
      this.suffix = suffix;
      this.outputLen = outputLen;
      this.enableXOF = enableXOF;
      this.rounds = rounds;
      anumber(outputLen);
      if (!(0 < blockLen && blockLen < 200))
        throw new Error("only keccak-f1600 function is supported");
      this.state = new Uint8Array(200);
      this.state32 = u32(this.state);
    }
    clone() {
      return this._cloneInto();
    }
    keccak() {
      swap32IfBE(this.state32);
      keccakP(this.state32, this.rounds);
      swap32IfBE(this.state32);
      this.posOut = 0;
      this.pos = 0;
    }
    update(data) {
      aexists(this);
      data = toBytes(data);
      abytes(data);
      const { blockLen, state } = this;
      const len = data.length;
      for (let pos = 0; pos < len; ) {
        const take = Math.min(blockLen - this.pos, len - pos);
        for (let i = 0; i < take; i++)
          state[this.pos++] ^= data[pos++];
        if (this.pos === blockLen)
          this.keccak();
      }
      return this;
    }
    finish() {
      if (this.finished)
        return;
      this.finished = true;
      const { state, suffix, pos, blockLen } = this;
      state[pos] ^= suffix;
      if ((suffix & 128) !== 0 && pos === blockLen - 1)
        this.keccak();
      state[blockLen - 1] ^= 128;
      this.keccak();
    }
    writeInto(out) {
      aexists(this, false);
      abytes(out);
      this.finish();
      const bufferOut = this.state;
      const { blockLen } = this;
      for (let pos = 0, len = out.length; pos < len; ) {
        if (this.posOut >= blockLen)
          this.keccak();
        const take = Math.min(blockLen - this.posOut, len - pos);
        out.set(bufferOut.subarray(this.posOut, this.posOut + take), pos);
        this.posOut += take;
        pos += take;
      }
      return out;
    }
    xofInto(out) {
      if (!this.enableXOF)
        throw new Error("XOF is not possible for this instance");
      return this.writeInto(out);
    }
    xof(bytes) {
      anumber(bytes);
      return this.xofInto(new Uint8Array(bytes));
    }
    digestInto(out) {
      aoutput(out, this);
      if (this.finished)
        throw new Error("digest() was already called");
      this.writeInto(out);
      this.destroy();
      return out;
    }
    digest() {
      return this.digestInto(new Uint8Array(this.outputLen));
    }
    destroy() {
      this.destroyed = true;
      clean(this.state);
    }
    _cloneInto(to) {
      const { blockLen, suffix, outputLen, rounds, enableXOF } = this;
      to || (to = new _Keccak(blockLen, suffix, outputLen, enableXOF, rounds));
      to.state32.set(this.state32);
      to.pos = this.pos;
      to.posOut = this.posOut;
      to.finished = this.finished;
      to.rounds = rounds;
      to.suffix = suffix;
      to.outputLen = outputLen;
      to.enableXOF = enableXOF;
      to.destroyed = this.destroyed;
      return to;
    }
  };
  var gen = (suffix, blockLen, outputLen) => createHasher(() => new Keccak(blockLen, suffix, outputLen));
  var sha3_224 = /* @__PURE__ */ (() => gen(6, 144, 224 / 8))();
  var sha3_256 = /* @__PURE__ */ (() => gen(6, 136, 256 / 8))();
  var sha3_384 = /* @__PURE__ */ (() => gen(6, 104, 384 / 8))();
  var sha3_512 = /* @__PURE__ */ (() => gen(6, 72, 512 / 8))();
  var genShake = (suffix, blockLen, outputLen) => createXOFer((opts2 = {}) => new Keccak(blockLen, suffix, opts2.dkLen === void 0 ? outputLen : opts2.dkLen, true));
  var shake128 = /* @__PURE__ */ (() => genShake(31, 168, 128 / 8))();
  var shake256 = /* @__PURE__ */ (() => genShake(31, 136, 256 / 8))();

  // node_modules/@noble/hashes/esm/_md.js
  function setBigUint64(view, byteOffset, value, isLE2) {
    if (typeof view.setBigUint64 === "function")
      return view.setBigUint64(byteOffset, value, isLE2);
    const _32n2 = BigInt(32);
    const _u32_max = BigInt(4294967295);
    const wh = Number(value >> _32n2 & _u32_max);
    const wl = Number(value & _u32_max);
    const h = isLE2 ? 4 : 0;
    const l = isLE2 ? 0 : 4;
    view.setUint32(byteOffset + h, wh, isLE2);
    view.setUint32(byteOffset + l, wl, isLE2);
  }
  function Chi(a, b, c) {
    return a & b ^ ~a & c;
  }
  function Maj(a, b, c) {
    return a & b ^ a & c ^ b & c;
  }
  var HashMD = class extends Hash {
    constructor(blockLen, outputLen, padOffset, isLE2) {
      super();
      this.finished = false;
      this.length = 0;
      this.pos = 0;
      this.destroyed = false;
      this.blockLen = blockLen;
      this.outputLen = outputLen;
      this.padOffset = padOffset;
      this.isLE = isLE2;
      this.buffer = new Uint8Array(blockLen);
      this.view = createView(this.buffer);
    }
    update(data) {
      aexists(this);
      data = toBytes(data);
      abytes(data);
      const { view, buffer, blockLen } = this;
      const len = data.length;
      for (let pos = 0; pos < len; ) {
        const take = Math.min(blockLen - this.pos, len - pos);
        if (take === blockLen) {
          const dataView = createView(data);
          for (; blockLen <= len - pos; pos += blockLen)
            this.process(dataView, pos);
          continue;
        }
        buffer.set(data.subarray(pos, pos + take), this.pos);
        this.pos += take;
        pos += take;
        if (this.pos === blockLen) {
          this.process(view, 0);
          this.pos = 0;
        }
      }
      this.length += data.length;
      this.roundClean();
      return this;
    }
    digestInto(out) {
      aexists(this);
      aoutput(out, this);
      this.finished = true;
      const { buffer, view, blockLen, isLE: isLE2 } = this;
      let { pos } = this;
      buffer[pos++] = 128;
      clean(this.buffer.subarray(pos));
      if (this.padOffset > blockLen - pos) {
        this.process(view, 0);
        pos = 0;
      }
      for (let i = pos; i < blockLen; i++)
        buffer[i] = 0;
      setBigUint64(view, blockLen - 8, BigInt(this.length * 8), isLE2);
      this.process(view, 0);
      const oview = createView(out);
      const len = this.outputLen;
      if (len % 4)
        throw new Error("_sha2: outputLen should be aligned to 32bit");
      const outLen = len / 4;
      const state = this.get();
      if (outLen > state.length)
        throw new Error("_sha2: outputLen bigger than state");
      for (let i = 0; i < outLen; i++)
        oview.setUint32(4 * i, state[i], isLE2);
    }
    digest() {
      const { buffer, outputLen } = this;
      this.digestInto(buffer);
      const res = buffer.slice(0, outputLen);
      this.destroy();
      return res;
    }
    _cloneInto(to) {
      to || (to = new this.constructor());
      to.set(...this.get());
      const { blockLen, buffer, length, finished, destroyed, pos } = this;
      to.destroyed = destroyed;
      to.finished = finished;
      to.length = length;
      to.pos = pos;
      if (length % blockLen)
        to.buffer.set(buffer);
      return to;
    }
    clone() {
      return this._cloneInto();
    }
  };
  var SHA256_IV = /* @__PURE__ */ Uint32Array.from([
    1779033703,
    3144134277,
    1013904242,
    2773480762,
    1359893119,
    2600822924,
    528734635,
    1541459225
  ]);
  var SHA224_IV = /* @__PURE__ */ Uint32Array.from([
    3238371032,
    914150663,
    812702999,
    4144912697,
    4290775857,
    1750603025,
    1694076839,
    3204075428
  ]);
  var SHA384_IV = /* @__PURE__ */ Uint32Array.from([
    3418070365,
    3238371032,
    1654270250,
    914150663,
    2438529370,
    812702999,
    355462360,
    4144912697,
    1731405415,
    4290775857,
    2394180231,
    1750603025,
    3675008525,
    1694076839,
    1203062813,
    3204075428
  ]);
  var SHA512_IV = /* @__PURE__ */ Uint32Array.from([
    1779033703,
    4089235720,
    3144134277,
    2227873595,
    1013904242,
    4271175723,
    2773480762,
    1595750129,
    1359893119,
    2917565137,
    2600822924,
    725511199,
    528734635,
    4215389547,
    1541459225,
    327033209
  ]);

  // node_modules/@noble/hashes/esm/sha2.js
  var SHA256_K = /* @__PURE__ */ Uint32Array.from([
    1116352408,
    1899447441,
    3049323471,
    3921009573,
    961987163,
    1508970993,
    2453635748,
    2870763221,
    3624381080,
    310598401,
    607225278,
    1426881987,
    1925078388,
    2162078206,
    2614888103,
    3248222580,
    3835390401,
    4022224774,
    264347078,
    604807628,
    770255983,
    1249150122,
    1555081692,
    1996064986,
    2554220882,
    2821834349,
    2952996808,
    3210313671,
    3336571891,
    3584528711,
    113926993,
    338241895,
    666307205,
    773529912,
    1294757372,
    1396182291,
    1695183700,
    1986661051,
    2177026350,
    2456956037,
    2730485921,
    2820302411,
    3259730800,
    3345764771,
    3516065817,
    3600352804,
    4094571909,
    275423344,
    430227734,
    506948616,
    659060556,
    883997877,
    958139571,
    1322822218,
    1537002063,
    1747873779,
    1955562222,
    2024104815,
    2227730452,
    2361852424,
    2428436474,
    2756734187,
    3204031479,
    3329325298
  ]);
  var SHA256_W = /* @__PURE__ */ new Uint32Array(64);
  var SHA256 = class extends HashMD {
    constructor(outputLen = 32) {
      super(64, outputLen, 8, false);
      this.A = SHA256_IV[0] | 0;
      this.B = SHA256_IV[1] | 0;
      this.C = SHA256_IV[2] | 0;
      this.D = SHA256_IV[3] | 0;
      this.E = SHA256_IV[4] | 0;
      this.F = SHA256_IV[5] | 0;
      this.G = SHA256_IV[6] | 0;
      this.H = SHA256_IV[7] | 0;
    }
    get() {
      const { A, B, C, D, E, F: F2, G, H } = this;
      return [A, B, C, D, E, F2, G, H];
    }
    // prettier-ignore
    set(A, B, C, D, E, F2, G, H) {
      this.A = A | 0;
      this.B = B | 0;
      this.C = C | 0;
      this.D = D | 0;
      this.E = E | 0;
      this.F = F2 | 0;
      this.G = G | 0;
      this.H = H | 0;
    }
    process(view, offset) {
      for (let i = 0; i < 16; i++, offset += 4)
        SHA256_W[i] = view.getUint32(offset, false);
      for (let i = 16; i < 64; i++) {
        const W15 = SHA256_W[i - 15];
        const W2 = SHA256_W[i - 2];
        const s0 = rotr(W15, 7) ^ rotr(W15, 18) ^ W15 >>> 3;
        const s1 = rotr(W2, 17) ^ rotr(W2, 19) ^ W2 >>> 10;
        SHA256_W[i] = s1 + SHA256_W[i - 7] + s0 + SHA256_W[i - 16] | 0;
      }
      let { A, B, C, D, E, F: F2, G, H } = this;
      for (let i = 0; i < 64; i++) {
        const sigma1 = rotr(E, 6) ^ rotr(E, 11) ^ rotr(E, 25);
        const T1 = H + sigma1 + Chi(E, F2, G) + SHA256_K[i] + SHA256_W[i] | 0;
        const sigma0 = rotr(A, 2) ^ rotr(A, 13) ^ rotr(A, 22);
        const T2 = sigma0 + Maj(A, B, C) | 0;
        H = G;
        G = F2;
        F2 = E;
        E = D + T1 | 0;
        D = C;
        C = B;
        B = A;
        A = T1 + T2 | 0;
      }
      A = A + this.A | 0;
      B = B + this.B | 0;
      C = C + this.C | 0;
      D = D + this.D | 0;
      E = E + this.E | 0;
      F2 = F2 + this.F | 0;
      G = G + this.G | 0;
      H = H + this.H | 0;
      this.set(A, B, C, D, E, F2, G, H);
    }
    roundClean() {
      clean(SHA256_W);
    }
    destroy() {
      this.set(0, 0, 0, 0, 0, 0, 0, 0);
      clean(this.buffer);
    }
  };
  var SHA224 = class extends SHA256 {
    constructor() {
      super(28);
      this.A = SHA224_IV[0] | 0;
      this.B = SHA224_IV[1] | 0;
      this.C = SHA224_IV[2] | 0;
      this.D = SHA224_IV[3] | 0;
      this.E = SHA224_IV[4] | 0;
      this.F = SHA224_IV[5] | 0;
      this.G = SHA224_IV[6] | 0;
      this.H = SHA224_IV[7] | 0;
    }
  };
  var K512 = /* @__PURE__ */ (() => split([
    "0x428a2f98d728ae22",
    "0x7137449123ef65cd",
    "0xb5c0fbcfec4d3b2f",
    "0xe9b5dba58189dbbc",
    "0x3956c25bf348b538",
    "0x59f111f1b605d019",
    "0x923f82a4af194f9b",
    "0xab1c5ed5da6d8118",
    "0xd807aa98a3030242",
    "0x12835b0145706fbe",
    "0x243185be4ee4b28c",
    "0x550c7dc3d5ffb4e2",
    "0x72be5d74f27b896f",
    "0x80deb1fe3b1696b1",
    "0x9bdc06a725c71235",
    "0xc19bf174cf692694",
    "0xe49b69c19ef14ad2",
    "0xefbe4786384f25e3",
    "0x0fc19dc68b8cd5b5",
    "0x240ca1cc77ac9c65",
    "0x2de92c6f592b0275",
    "0x4a7484aa6ea6e483",
    "0x5cb0a9dcbd41fbd4",
    "0x76f988da831153b5",
    "0x983e5152ee66dfab",
    "0xa831c66d2db43210",
    "0xb00327c898fb213f",
    "0xbf597fc7beef0ee4",
    "0xc6e00bf33da88fc2",
    "0xd5a79147930aa725",
    "0x06ca6351e003826f",
    "0x142929670a0e6e70",
    "0x27b70a8546d22ffc",
    "0x2e1b21385c26c926",
    "0x4d2c6dfc5ac42aed",
    "0x53380d139d95b3df",
    "0x650a73548baf63de",
    "0x766a0abb3c77b2a8",
    "0x81c2c92e47edaee6",
    "0x92722c851482353b",
    "0xa2bfe8a14cf10364",
    "0xa81a664bbc423001",
    "0xc24b8b70d0f89791",
    "0xc76c51a30654be30",
    "0xd192e819d6ef5218",
    "0xd69906245565a910",
    "0xf40e35855771202a",
    "0x106aa07032bbd1b8",
    "0x19a4c116b8d2d0c8",
    "0x1e376c085141ab53",
    "0x2748774cdf8eeb99",
    "0x34b0bcb5e19b48a8",
    "0x391c0cb3c5c95a63",
    "0x4ed8aa4ae3418acb",
    "0x5b9cca4f7763e373",
    "0x682e6ff3d6b2b8a3",
    "0x748f82ee5defb2fc",
    "0x78a5636f43172f60",
    "0x84c87814a1f0ab72",
    "0x8cc702081a6439ec",
    "0x90befffa23631e28",
    "0xa4506cebde82bde9",
    "0xbef9a3f7b2c67915",
    "0xc67178f2e372532b",
    "0xca273eceea26619c",
    "0xd186b8c721c0c207",
    "0xeada7dd6cde0eb1e",
    "0xf57d4f7fee6ed178",
    "0x06f067aa72176fba",
    "0x0a637dc5a2c898a6",
    "0x113f9804bef90dae",
    "0x1b710b35131c471b",
    "0x28db77f523047d84",
    "0x32caab7b40c72493",
    "0x3c9ebe0a15c9bebc",
    "0x431d67c49c100d4c",
    "0x4cc5d4becb3e42b6",
    "0x597f299cfc657e2a",
    "0x5fcb6fab3ad6faec",
    "0x6c44198c4a475817"
  ].map((n) => BigInt(n))))();
  var SHA512_Kh = /* @__PURE__ */ (() => K512[0])();
  var SHA512_Kl = /* @__PURE__ */ (() => K512[1])();
  var SHA512_W_H = /* @__PURE__ */ new Uint32Array(80);
  var SHA512_W_L = /* @__PURE__ */ new Uint32Array(80);
  var SHA512 = class extends HashMD {
    constructor(outputLen = 64) {
      super(128, outputLen, 16, false);
      this.Ah = SHA512_IV[0] | 0;
      this.Al = SHA512_IV[1] | 0;
      this.Bh = SHA512_IV[2] | 0;
      this.Bl = SHA512_IV[3] | 0;
      this.Ch = SHA512_IV[4] | 0;
      this.Cl = SHA512_IV[5] | 0;
      this.Dh = SHA512_IV[6] | 0;
      this.Dl = SHA512_IV[7] | 0;
      this.Eh = SHA512_IV[8] | 0;
      this.El = SHA512_IV[9] | 0;
      this.Fh = SHA512_IV[10] | 0;
      this.Fl = SHA512_IV[11] | 0;
      this.Gh = SHA512_IV[12] | 0;
      this.Gl = SHA512_IV[13] | 0;
      this.Hh = SHA512_IV[14] | 0;
      this.Hl = SHA512_IV[15] | 0;
    }
    // prettier-ignore
    get() {
      const { Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl } = this;
      return [Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl];
    }
    // prettier-ignore
    set(Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl) {
      this.Ah = Ah | 0;
      this.Al = Al | 0;
      this.Bh = Bh | 0;
      this.Bl = Bl | 0;
      this.Ch = Ch | 0;
      this.Cl = Cl | 0;
      this.Dh = Dh | 0;
      this.Dl = Dl | 0;
      this.Eh = Eh | 0;
      this.El = El | 0;
      this.Fh = Fh | 0;
      this.Fl = Fl | 0;
      this.Gh = Gh | 0;
      this.Gl = Gl | 0;
      this.Hh = Hh | 0;
      this.Hl = Hl | 0;
    }
    process(view, offset) {
      for (let i = 0; i < 16; i++, offset += 4) {
        SHA512_W_H[i] = view.getUint32(offset);
        SHA512_W_L[i] = view.getUint32(offset += 4);
      }
      for (let i = 16; i < 80; i++) {
        const W15h = SHA512_W_H[i - 15] | 0;
        const W15l = SHA512_W_L[i - 15] | 0;
        const s0h = rotrSH(W15h, W15l, 1) ^ rotrSH(W15h, W15l, 8) ^ shrSH(W15h, W15l, 7);
        const s0l = rotrSL(W15h, W15l, 1) ^ rotrSL(W15h, W15l, 8) ^ shrSL(W15h, W15l, 7);
        const W2h = SHA512_W_H[i - 2] | 0;
        const W2l = SHA512_W_L[i - 2] | 0;
        const s1h = rotrSH(W2h, W2l, 19) ^ rotrBH(W2h, W2l, 61) ^ shrSH(W2h, W2l, 6);
        const s1l = rotrSL(W2h, W2l, 19) ^ rotrBL(W2h, W2l, 61) ^ shrSL(W2h, W2l, 6);
        const SUMl = add4L(s0l, s1l, SHA512_W_L[i - 7], SHA512_W_L[i - 16]);
        const SUMh = add4H(SUMl, s0h, s1h, SHA512_W_H[i - 7], SHA512_W_H[i - 16]);
        SHA512_W_H[i] = SUMh | 0;
        SHA512_W_L[i] = SUMl | 0;
      }
      let { Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl } = this;
      for (let i = 0; i < 80; i++) {
        const sigma1h = rotrSH(Eh, El, 14) ^ rotrSH(Eh, El, 18) ^ rotrBH(Eh, El, 41);
        const sigma1l = rotrSL(Eh, El, 14) ^ rotrSL(Eh, El, 18) ^ rotrBL(Eh, El, 41);
        const CHIh = Eh & Fh ^ ~Eh & Gh;
        const CHIl = El & Fl ^ ~El & Gl;
        const T1ll = add5L(Hl, sigma1l, CHIl, SHA512_Kl[i], SHA512_W_L[i]);
        const T1h = add5H(T1ll, Hh, sigma1h, CHIh, SHA512_Kh[i], SHA512_W_H[i]);
        const T1l = T1ll | 0;
        const sigma0h = rotrSH(Ah, Al, 28) ^ rotrBH(Ah, Al, 34) ^ rotrBH(Ah, Al, 39);
        const sigma0l = rotrSL(Ah, Al, 28) ^ rotrBL(Ah, Al, 34) ^ rotrBL(Ah, Al, 39);
        const MAJh = Ah & Bh ^ Ah & Ch ^ Bh & Ch;
        const MAJl = Al & Bl ^ Al & Cl ^ Bl & Cl;
        Hh = Gh | 0;
        Hl = Gl | 0;
        Gh = Fh | 0;
        Gl = Fl | 0;
        Fh = Eh | 0;
        Fl = El | 0;
        ({ h: Eh, l: El } = add(Dh | 0, Dl | 0, T1h | 0, T1l | 0));
        Dh = Ch | 0;
        Dl = Cl | 0;
        Ch = Bh | 0;
        Cl = Bl | 0;
        Bh = Ah | 0;
        Bl = Al | 0;
        const All = add3L(T1l, sigma0l, MAJl);
        Ah = add3H(All, T1h, sigma0h, MAJh);
        Al = All | 0;
      }
      ({ h: Ah, l: Al } = add(this.Ah | 0, this.Al | 0, Ah | 0, Al | 0));
      ({ h: Bh, l: Bl } = add(this.Bh | 0, this.Bl | 0, Bh | 0, Bl | 0));
      ({ h: Ch, l: Cl } = add(this.Ch | 0, this.Cl | 0, Ch | 0, Cl | 0));
      ({ h: Dh, l: Dl } = add(this.Dh | 0, this.Dl | 0, Dh | 0, Dl | 0));
      ({ h: Eh, l: El } = add(this.Eh | 0, this.El | 0, Eh | 0, El | 0));
      ({ h: Fh, l: Fl } = add(this.Fh | 0, this.Fl | 0, Fh | 0, Fl | 0));
      ({ h: Gh, l: Gl } = add(this.Gh | 0, this.Gl | 0, Gh | 0, Gl | 0));
      ({ h: Hh, l: Hl } = add(this.Hh | 0, this.Hl | 0, Hh | 0, Hl | 0));
      this.set(Ah, Al, Bh, Bl, Ch, Cl, Dh, Dl, Eh, El, Fh, Fl, Gh, Gl, Hh, Hl);
    }
    roundClean() {
      clean(SHA512_W_H, SHA512_W_L);
    }
    destroy() {
      clean(this.buffer);
      this.set(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }
  };
  var SHA384 = class extends SHA512 {
    constructor() {
      super(48);
      this.Ah = SHA384_IV[0] | 0;
      this.Al = SHA384_IV[1] | 0;
      this.Bh = SHA384_IV[2] | 0;
      this.Bl = SHA384_IV[3] | 0;
      this.Ch = SHA384_IV[4] | 0;
      this.Cl = SHA384_IV[5] | 0;
      this.Dh = SHA384_IV[6] | 0;
      this.Dl = SHA384_IV[7] | 0;
      this.Eh = SHA384_IV[8] | 0;
      this.El = SHA384_IV[9] | 0;
      this.Fh = SHA384_IV[10] | 0;
      this.Fl = SHA384_IV[11] | 0;
      this.Gh = SHA384_IV[12] | 0;
      this.Gl = SHA384_IV[13] | 0;
      this.Hh = SHA384_IV[14] | 0;
      this.Hl = SHA384_IV[15] | 0;
    }
  };
  var T224_IV = /* @__PURE__ */ Uint32Array.from([
    2352822216,
    424955298,
    1944164710,
    2312950998,
    502970286,
    855612546,
    1738396948,
    1479516111,
    258812777,
    2077511080,
    2011393907,
    79989058,
    1067287976,
    1780299464,
    286451373,
    2446758561
  ]);
  var T256_IV = /* @__PURE__ */ Uint32Array.from([
    573645204,
    4230739756,
    2673172387,
    3360449730,
    596883563,
    1867755857,
    2520282905,
    1497426621,
    2519219938,
    2827943907,
    3193839141,
    1401305490,
    721525244,
    746961066,
    246885852,
    2177182882
  ]);
  var SHA512_224 = class extends SHA512 {
    constructor() {
      super(28);
      this.Ah = T224_IV[0] | 0;
      this.Al = T224_IV[1] | 0;
      this.Bh = T224_IV[2] | 0;
      this.Bl = T224_IV[3] | 0;
      this.Ch = T224_IV[4] | 0;
      this.Cl = T224_IV[5] | 0;
      this.Dh = T224_IV[6] | 0;
      this.Dl = T224_IV[7] | 0;
      this.Eh = T224_IV[8] | 0;
      this.El = T224_IV[9] | 0;
      this.Fh = T224_IV[10] | 0;
      this.Fl = T224_IV[11] | 0;
      this.Gh = T224_IV[12] | 0;
      this.Gl = T224_IV[13] | 0;
      this.Hh = T224_IV[14] | 0;
      this.Hl = T224_IV[15] | 0;
    }
  };
  var SHA512_256 = class extends SHA512 {
    constructor() {
      super(32);
      this.Ah = T256_IV[0] | 0;
      this.Al = T256_IV[1] | 0;
      this.Bh = T256_IV[2] | 0;
      this.Bl = T256_IV[3] | 0;
      this.Ch = T256_IV[4] | 0;
      this.Cl = T256_IV[5] | 0;
      this.Dh = T256_IV[6] | 0;
      this.Dl = T256_IV[7] | 0;
      this.Eh = T256_IV[8] | 0;
      this.El = T256_IV[9] | 0;
      this.Fh = T256_IV[10] | 0;
      this.Fl = T256_IV[11] | 0;
      this.Gh = T256_IV[12] | 0;
      this.Gl = T256_IV[13] | 0;
      this.Hh = T256_IV[14] | 0;
      this.Hl = T256_IV[15] | 0;
    }
  };
  var sha256 = /* @__PURE__ */ createHasher(() => new SHA256());
  var sha224 = /* @__PURE__ */ createHasher(() => new SHA224());
  var sha512 = /* @__PURE__ */ createHasher(() => new SHA512());
  var sha384 = /* @__PURE__ */ createHasher(() => new SHA384());
  var sha512_256 = /* @__PURE__ */ createHasher(() => new SHA512_256());
  var sha512_224 = /* @__PURE__ */ createHasher(() => new SHA512_224());

  // node_modules/@noble/post-quantum/esm/utils.js
  var ensureBytes = abytes;
  var randomBytes2 = randomBytes;
  function equalBytes(a, b) {
    if (a.length !== b.length)
      return false;
    let diff = 0;
    for (let i = 0; i < a.length; i++)
      diff |= a[i] ^ b[i];
    return diff === 0;
  }
  function splitCoder(...lengths) {
    const getLength = (c) => typeof c === "number" ? c : c.bytesLen;
    const bytesLen = lengths.reduce((sum, a) => sum + getLength(a), 0);
    return {
      bytesLen,
      encode: (bufs) => {
        const res = new Uint8Array(bytesLen);
        for (let i = 0, pos = 0; i < lengths.length; i++) {
          const c = lengths[i];
          const l = getLength(c);
          const b = typeof c === "number" ? bufs[i] : c.encode(bufs[i]);
          ensureBytes(b, l);
          res.set(b, pos);
          if (typeof c !== "number")
            b.fill(0);
          pos += l;
        }
        return res;
      },
      decode: (buf) => {
        ensureBytes(buf, bytesLen);
        const res = [];
        for (const c of lengths) {
          const l = getLength(c);
          const b = buf.subarray(0, l);
          res.push(typeof c === "number" ? b : c.decode(b));
          buf = buf.subarray(l);
        }
        return res;
      }
    };
  }
  function vecCoder(c, vecLen) {
    const bytesLen = vecLen * c.bytesLen;
    return {
      bytesLen,
      encode: (u) => {
        if (u.length !== vecLen)
          throw new Error(`vecCoder.encode: wrong length=${u.length}. Expected: ${vecLen}`);
        const res = new Uint8Array(bytesLen);
        for (let i = 0, pos = 0; i < u.length; i++) {
          const b = c.encode(u[i]);
          res.set(b, pos);
          b.fill(0);
          pos += b.length;
        }
        return res;
      },
      decode: (a) => {
        ensureBytes(a, bytesLen);
        const r = [];
        for (let i = 0; i < a.length; i += c.bytesLen)
          r.push(c.decode(a.subarray(i, i + c.bytesLen)));
        return r;
      }
    };
  }
  function cleanBytes(...list) {
    for (const t of list) {
      if (Array.isArray(t))
        for (const b of t)
          b.fill(0);
      else
        t.fill(0);
    }
  }
  function getMask(bits) {
    return (1 << bits) - 1;
  }
  var EMPTY = new Uint8Array(0);
  var HASHES = {
    "SHA2-256": { oid: hexToBytes("0609608648016503040201"), hash: sha256 },
    "SHA2-384": { oid: hexToBytes("0609608648016503040202"), hash: sha384 },
    "SHA2-512": { oid: hexToBytes("0609608648016503040203"), hash: sha512 },
    "SHA2-224": { oid: hexToBytes("0609608648016503040204"), hash: sha224 },
    "SHA2-512/224": { oid: hexToBytes("0609608648016503040205"), hash: sha512_224 },
    "SHA2-512/256": { oid: hexToBytes("0609608648016503040206"), hash: sha512_256 },
    "SHA3-224": { oid: hexToBytes("0609608648016503040207"), hash: sha3_224 },
    "SHA3-256": { oid: hexToBytes("0609608648016503040208"), hash: sha3_256 },
    "SHA3-384": { oid: hexToBytes("0609608648016503040209"), hash: sha3_384 },
    "SHA3-512": { oid: hexToBytes("060960864801650304020A"), hash: sha3_512 },
    "SHAKE-128": {
      oid: hexToBytes("060960864801650304020B"),
      hash: (msg) => shake128(msg, { dkLen: 32 })
    },
    "SHAKE-256": {
      oid: hexToBytes("060960864801650304020C"),
      hash: (msg) => shake256(msg, { dkLen: 64 })
    }
  };

  // node_modules/@noble/post-quantum/esm/_crystals.js
  function bitReversal(n, bits = 8) {
    const padded = n.toString(2).padStart(8, "0");
    const sliced = padded.slice(-bits).padStart(7, "0");
    const revrsd = sliced.split("").reverse().join("");
    return Number.parseInt(revrsd, 2);
  }
  var genCrystals = (opts2) => {
    const { newPoly, N: N2, Q: Q2, F: F2, ROOT_OF_UNITY: ROOT_OF_UNITY2, brvBits, isKyber } = opts2;
    const mod2 = (a, modulo = Q2) => {
      const result = a % modulo | 0;
      return (result >= 0 ? result | 0 : modulo + result | 0) | 0;
    };
    const smod = (a, modulo = Q2) => {
      const r = mod2(a, modulo) | 0;
      return (r > modulo >> 1 ? r - modulo | 0 : r) | 0;
    };
    function getZettas() {
      const out = newPoly(N2);
      for (let i = 0; i < N2; i++) {
        const b = bitReversal(i, brvBits);
        const p = BigInt(ROOT_OF_UNITY2) ** BigInt(b) % BigInt(Q2);
        out[i] = Number(p) | 0;
      }
      return out;
    }
    const nttZetas2 = getZettas();
    const LEN1 = isKyber ? 128 : N2;
    const LEN2 = isKyber ? 1 : 0;
    const NTT2 = {
      encode: (r) => {
        for (let k = 1, len = 128; len > LEN2; len >>= 1) {
          for (let start = 0; start < N2; start += 2 * len) {
            const zeta = nttZetas2[k++];
            for (let j = start; j < start + len; j++) {
              const t = mod2(zeta * r[j + len]);
              r[j + len] = mod2(r[j] - t) | 0;
              r[j] = mod2(r[j] + t) | 0;
            }
          }
        }
        return r;
      },
      decode: (r) => {
        for (let k = LEN1 - 1, len = 1 + LEN2; len < LEN1 + LEN2; len <<= 1) {
          for (let start = 0; start < N2; start += 2 * len) {
            const zeta = nttZetas2[k--];
            for (let j = start; j < start + len; j++) {
              const t = r[j];
              r[j] = mod2(t + r[j + len]);
              r[j + len] = mod2(zeta * (r[j + len] - t));
            }
          }
        }
        for (let i = 0; i < r.length; i++)
          r[i] = mod2(F2 * r[i]);
        return r;
      }
    };
    const bitsCoder2 = (d, c) => {
      const mask = getMask(d);
      const bytesLen = d * (N2 / 8);
      return {
        bytesLen,
        encode: (poly) => {
          const r = new Uint8Array(bytesLen);
          for (let i = 0, buf = 0, bufLen = 0, pos = 0; i < poly.length; i++) {
            buf |= (c.encode(poly[i]) & mask) << bufLen;
            bufLen += d;
            for (; bufLen >= 8; bufLen -= 8, buf >>= 8)
              r[pos++] = buf & getMask(bufLen);
          }
          return r;
        },
        decode: (bytes) => {
          const r = newPoly(N2);
          for (let i = 0, buf = 0, bufLen = 0, pos = 0; i < bytes.length; i++) {
            buf |= bytes[i] << bufLen;
            bufLen += 8;
            for (; bufLen >= d; bufLen -= d, buf >>= d)
              r[pos++] = c.decode(buf & mask);
          }
          return r;
        }
      };
    };
    return { mod: mod2, smod, nttZetas: nttZetas2, NTT: NTT2, bitsCoder: bitsCoder2 };
  };
  var createXofShake = (shake) => (seed, blockLen) => {
    if (!blockLen)
      blockLen = shake.blockLen;
    const _seed = new Uint8Array(seed.length + 2);
    _seed.set(seed);
    const seedLen = seed.length;
    const buf = new Uint8Array(blockLen);
    let h = shake.create({});
    let calls = 0;
    let xofs = 0;
    return {
      stats: () => ({ calls, xofs }),
      get: (x, y) => {
        _seed[seedLen + 0] = x;
        _seed[seedLen + 1] = y;
        h.destroy();
        h = shake.create({}).update(_seed);
        calls++;
        return () => {
          xofs++;
          return h.xofInto(buf);
        };
      },
      clean: () => {
        h.destroy();
        buf.fill(0);
        _seed.fill(0);
      }
    };
  };
  var XOF128 = /* @__PURE__ */ createXofShake(shake128);

  // node_modules/@noble/post-quantum/esm/ml-kem.js
  var N = 256;
  var Q = 3329;
  var F = 3303;
  var ROOT_OF_UNITY = 17;
  var { mod, nttZetas, NTT, bitsCoder } = genCrystals({
    N,
    Q,
    F,
    ROOT_OF_UNITY,
    newPoly: (n) => new Uint16Array(n),
    brvBits: 7,
    isKyber: true
  });
  var PARAMS = {
    512: { N, Q, K: 2, ETA1: 3, ETA2: 2, du: 10, dv: 4, RBGstrength: 128 },
    768: { N, Q, K: 3, ETA1: 2, ETA2: 2, du: 10, dv: 4, RBGstrength: 192 },
    1024: { N, Q, K: 4, ETA1: 2, ETA2: 2, du: 11, dv: 5, RBGstrength: 256 }
  };
  var compress = (d) => {
    if (d >= 12)
      return { encode: (i) => i, decode: (i) => i };
    const a = 2 ** (d - 1);
    return {
      // const compress = (i: number) => round((2 ** d / Q) * i) % 2 ** d;
      encode: (i) => ((i << d) + Q / 2) / Q,
      // const decompress = (i: number) => round((Q / 2 ** d) * i);
      decode: (i) => i * Q + a >>> d
    };
  };
  var polyCoder = (d) => bitsCoder(d, compress(d));
  function polyAdd(a, b) {
    for (let i = 0; i < N; i++)
      a[i] = mod(a[i] + b[i]);
  }
  function polySub(a, b) {
    for (let i = 0; i < N; i++)
      a[i] = mod(a[i] - b[i]);
  }
  function BaseCaseMultiply(a0, a1, b0, b1, zeta) {
    const c0 = mod(a1 * b1 * zeta + a0 * b0);
    const c1 = mod(a0 * b1 + a1 * b0);
    return { c0, c1 };
  }
  function MultiplyNTTs(f, g) {
    for (let i = 0; i < N / 2; i++) {
      let z = nttZetas[64 + (i >> 1)];
      if (i & 1)
        z = -z;
      const { c0, c1 } = BaseCaseMultiply(f[2 * i + 0], f[2 * i + 1], g[2 * i + 0], g[2 * i + 1], z);
      f[2 * i + 0] = c0;
      f[2 * i + 1] = c1;
    }
    return f;
  }
  function SampleNTT(xof) {
    const r = new Uint16Array(N);
    for (let j = 0; j < N; ) {
      const b = xof();
      if (b.length % 3)
        throw new Error("SampleNTT: unaligned block");
      for (let i = 0; j < N && i + 3 <= b.length; i += 3) {
        const d1 = (b[i + 0] >> 0 | b[i + 1] << 8) & 4095;
        const d2 = (b[i + 1] >> 4 | b[i + 2] << 4) & 4095;
        if (d1 < Q)
          r[j++] = d1;
        if (j < N && d2 < Q)
          r[j++] = d2;
      }
    }
    return r;
  }
  function sampleCBD(PRF, seed, nonce, eta) {
    const buf = PRF(eta * N / 4, seed, nonce);
    const r = new Uint16Array(N);
    const b32 = u32(buf);
    let len = 0;
    for (let i = 0, p = 0, bb = 0, t0 = 0; i < b32.length; i++) {
      let b = b32[i];
      for (let j = 0; j < 32; j++) {
        bb += b & 1;
        b >>= 1;
        len += 1;
        if (len === eta) {
          t0 = bb;
          bb = 0;
        } else if (len === 2 * eta) {
          r[p++] = mod(t0 - bb);
          bb = 0;
          len = 0;
        }
      }
    }
    if (len)
      throw new Error(`sampleCBD: leftover bits: ${len}`);
    return r;
  }
  var genKPKE = (opts2) => {
    const { K, PRF, XOF, HASH512, ETA1, ETA2, du, dv } = opts2;
    const poly1 = polyCoder(1);
    const polyV = polyCoder(dv);
    const polyU = polyCoder(du);
    const publicCoder = splitCoder(vecCoder(polyCoder(12), K), 32);
    const secretCoder = vecCoder(polyCoder(12), K);
    const cipherCoder = splitCoder(vecCoder(polyU, K), polyV);
    const seedCoder = splitCoder(32, 32);
    return {
      secretCoder,
      secretKeyLen: secretCoder.bytesLen,
      publicKeyLen: publicCoder.bytesLen,
      cipherTextLen: cipherCoder.bytesLen,
      keygen: (seed) => {
        ensureBytes(seed, 32);
        const seedDst = new Uint8Array(33);
        seedDst.set(seed);
        seedDst[32] = K;
        const seedHash = HASH512(seedDst);
        const [rho, sigma] = seedCoder.decode(seedHash);
        const sHat = [];
        const tHat = [];
        for (let i = 0; i < K; i++)
          sHat.push(NTT.encode(sampleCBD(PRF, sigma, i, ETA1)));
        const x = XOF(rho);
        for (let i = 0; i < K; i++) {
          const e = NTT.encode(sampleCBD(PRF, sigma, K + i, ETA1));
          for (let j = 0; j < K; j++) {
            const aji = SampleNTT(x.get(j, i));
            polyAdd(e, MultiplyNTTs(aji, sHat[j]));
          }
          tHat.push(e);
        }
        x.clean();
        const res = {
          publicKey: publicCoder.encode([tHat, rho]),
          secretKey: secretCoder.encode(sHat)
        };
        cleanBytes(rho, sigma, sHat, tHat, seedDst, seedHash);
        return res;
      },
      encrypt: (publicKey, msg, seed) => {
        const [tHat, rho] = publicCoder.decode(publicKey);
        const rHat = [];
        for (let i = 0; i < K; i++)
          rHat.push(NTT.encode(sampleCBD(PRF, seed, i, ETA1)));
        const x = XOF(rho);
        const tmp2 = new Uint16Array(N);
        const u = [];
        for (let i = 0; i < K; i++) {
          const e1 = sampleCBD(PRF, seed, K + i, ETA2);
          const tmp = new Uint16Array(N);
          for (let j = 0; j < K; j++) {
            const aij = SampleNTT(x.get(i, j));
            polyAdd(tmp, MultiplyNTTs(aij, rHat[j]));
          }
          polyAdd(e1, NTT.decode(tmp));
          u.push(e1);
          polyAdd(tmp2, MultiplyNTTs(tHat[i], rHat[i]));
          tmp.fill(0);
        }
        x.clean();
        const e2 = sampleCBD(PRF, seed, 2 * K, ETA2);
        polyAdd(e2, NTT.decode(tmp2));
        const v = poly1.decode(msg);
        polyAdd(v, e2);
        cleanBytes(tHat, rHat, tmp2, e2);
        return cipherCoder.encode([u, v]);
      },
      decrypt: (cipherText, privateKey) => {
        const [u, v] = cipherCoder.decode(cipherText);
        const sk = secretCoder.decode(privateKey);
        const tmp = new Uint16Array(N);
        for (let i = 0; i < K; i++)
          polyAdd(tmp, MultiplyNTTs(sk[i], NTT.encode(u[i])));
        polySub(v, NTT.decode(tmp));
        cleanBytes(tmp, sk, u);
        return poly1.encode(v);
      }
    };
  };
  function createKyber(opts2) {
    const KPKE = genKPKE(opts2);
    const { HASH256, HASH512, KDF } = opts2;
    const { secretCoder: KPKESecretCoder, cipherTextLen } = KPKE;
    const publicKeyLen = KPKE.publicKeyLen;
    const secretCoder = splitCoder(KPKE.secretKeyLen, KPKE.publicKeyLen, 32, 32);
    const secretKeyLen = secretCoder.bytesLen;
    const msgLen = 32;
    return {
      publicKeyLen,
      msgLen,
      keygen: (seed = randomBytes2(64)) => {
        ensureBytes(seed, 64);
        const { publicKey, secretKey: sk } = KPKE.keygen(seed.subarray(0, 32));
        const publicKeyHash = HASH256(publicKey);
        const secretKey = secretCoder.encode([sk, publicKey, publicKeyHash, seed.subarray(32)]);
        cleanBytes(sk, publicKeyHash);
        return { publicKey, secretKey };
      },
      encapsulate: (publicKey, msg = randomBytes2(32)) => {
        ensureBytes(publicKey, publicKeyLen);
        ensureBytes(msg, msgLen);
        const eke = publicKey.subarray(0, 384 * opts2.K);
        const ek = KPKESecretCoder.encode(KPKESecretCoder.decode(eke.slice()));
        if (!equalBytes(ek, eke)) {
          cleanBytes(ek);
          throw new Error("ML-KEM.encapsulate: wrong publicKey modulus");
        }
        cleanBytes(ek);
        const kr = HASH512.create().update(msg).update(HASH256(publicKey)).digest();
        const cipherText = KPKE.encrypt(publicKey, msg, kr.subarray(32, 64));
        kr.subarray(32).fill(0);
        return { cipherText, sharedSecret: kr.subarray(0, 32) };
      },
      decapsulate: (cipherText, secretKey) => {
        ensureBytes(secretKey, secretKeyLen);
        ensureBytes(cipherText, cipherTextLen);
        const [sk, publicKey, publicKeyHash, z] = secretCoder.decode(secretKey);
        const msg = KPKE.decrypt(cipherText, sk);
        const kr = HASH512.create().update(msg).update(publicKeyHash).digest();
        const Khat = kr.subarray(0, 32);
        const cipherText2 = KPKE.encrypt(publicKey, msg, kr.subarray(32, 64));
        const isValid = equalBytes(cipherText, cipherText2);
        const Kbar = KDF.create({ dkLen: 32 }).update(z).update(cipherText).digest();
        cleanBytes(msg, cipherText2, !isValid ? Khat : Kbar);
        return isValid ? Khat : Kbar;
      }
    };
  }
  function shakePRF(dkLen, key, nonce) {
    return shake256.create({ dkLen }).update(key).update(new Uint8Array([nonce])).digest();
  }
  var opts = {
    HASH256: sha3_256,
    HASH512: sha3_512,
    KDF: shake256,
    XOF: XOF128,
    PRF: shakePRF
  };
  var ml_kem512 = /* @__PURE__ */ createKyber({
    ...opts,
    ...PARAMS[512]
  });
  var ml_kem768 = /* @__PURE__ */ createKyber({
    ...opts,
    ...PARAMS[768]
  });
  var ml_kem1024 = /* @__PURE__ */ createKyber({
    ...opts,
    ...PARAMS[1024]
  });

  // entry.js
  globalThis.ml_kem768 = ml_kem768;
})();
/*! Bundled license information:

@noble/hashes/esm/utils.js:
  (*! noble-hashes - MIT License (c) 2022 Paul Miller (paulmillr.com) *)

@noble/post-quantum/esm/utils.js:
@noble/post-quantum/esm/_crystals.js:
@noble/post-quantum/esm/ml-kem.js:
  (*! noble-post-quantum - MIT License (c) 2024 Paul Miller (paulmillr.com) *)
*/


    // After loading Noble library, create the GraalVM API
    
    // Helper functions for hex conversion  
    const bytesToHex = (bytes) => {
        return Array.from(bytes, byte => byte.toString(16).padStart(2, '0')).join('');
    };

    const hexToBytes = (hex) => {
        const bytes = new Uint8Array(hex.length / 2);
        for (let i = 0; i < hex.length; i += 2) {
            bytes[i / 2] = parseInt(hex.substr(i, 2), 16);
        }
        return bytes;
    };

    // Main API object for GraalVM
    const NobleMLKEM = {
        // Generate key pair from seed (deterministic)
        generateKeyPairFromSeed: function(seedHex) {
            // Use the exact same conversion method as JavaScript SDK
            const seed = new Uint8Array(64);
            for (let i = 0; i < 64; i++) {
                seed[i] = parseInt(seedHex.substr(i * 2, 2), 16);
            }
            
            // Validate exactly like Noble library expects
            if (seed.length !== 64) {
                throw new Error('Seed must be exactly 64 bytes, got ' + seed.length);
            }
            
            // Generate deterministic key pair - exact same call as JavaScript SDK
            const keyPair = globalThis.ml_kem768.keygen(seed);
            
            return {
                publicKey: Array.from(keyPair.publicKey, byte => byte.toString(16).padStart(2, '0')).join(''),
                secretKey: Array.from(keyPair.secretKey, byte => byte.toString(16).padStart(2, '0')).join('')
            };
        },

        // Encapsulate to create shared secret
        encapsulate: function(publicKeyHex) {
            const publicKey = hexToBytes(publicKeyHex);
            const result = globalThis.ml_kem768.encapsulate(publicKey);
            return {
                sharedSecret: bytesToHex(result.sharedSecret),
                cipherText: bytesToHex(result.cipherText)
            };
        },

        // Decapsulate to recover shared secret
        decapsulate: function(cipherTextHex, secretKeyHex) {
            const cipherText = hexToBytes(cipherTextHex);
            const secretKey = hexToBytes(secretKeyHex);
            const sharedSecret = globalThis.ml_kem768.decapsulate(cipherText, secretKey);
            return bytesToHex(sharedSecret);
        },

        // Generate seed using SHAKE256 (identical to JavaScript SDK generateSecret)
        generateSecret: function(input, lengthInBits) {
            // Replicate the exact JavaScript SDK generateSecret logic
            // This ensures identical behavior across both SDKs
            
            // Simple string to bytes conversion for GraalVM compatibility
            const inputBytes = new Uint8Array(input.length);
            for (let i = 0; i < input.length; i++) {
                inputBytes[i] = input.charCodeAt(i);
            }
            
            const outputBytes = lengthInBits / 8; // Convert bits to bytes
            
            // Use the bundled shake256 implementation
            // Find the shake256 function from the bundle
            let shake256Function = null;
            if (typeof shake256 !== 'undefined') {
                shake256Function = shake256;
            } else if (typeof global !== 'undefined' && global.shake256) {
                shake256Function = global.shake256;
            } else {
                // Fallback to the ML-KEM internal shake implementation
                throw new Error('SHAKE256 implementation not found');
            }
            
            const hash = shake256Function.create({ dkLen: outputBytes });
            hash.update(inputBytes);
            const output = hash.digest();
            
            return bytesToHex(output);
        }
    };

    // Export for use in GraalJS
    global.NobleMLKEM = NobleMLKEM;

})(typeof globalThis !== 'undefined' ? globalThis : this);