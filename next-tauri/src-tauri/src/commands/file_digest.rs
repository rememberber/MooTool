//! MD5, SHA-1, and SM3. The rounds match `src/features/crypto/streamingDigest.ts`.

const MD5_SHIFTS: [u32; 64] = [
    7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 7, 12, 17, 22, 5, 9, 14, 20, 5, 9, 14, 20, 5, 9,
    14, 20, 5, 9, 14, 20, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 4, 11, 16, 23, 6, 10, 15, 21,
    6, 10, 15, 21, 6, 10, 15, 21, 6, 10, 15, 21,
];

const MD5_K: [u32; 64] = [
    0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
    0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
    0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
    0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
    0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
    0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
    0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
    0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391,
];

pub(super) trait StreamingDigest {
    fn update(&mut self, data: &[u8]);
    fn finalize(self) -> String;
}

pub(super) struct Md5 {
    state: [u32; 4],
    buffer: [u8; 64],
    offset: usize,
    length: u64,
}

impl Default for Md5 {
    fn default() -> Self {
        Self {
            state: [0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476],
            buffer: [0; 64],
            offset: 0,
            length: 0,
        }
    }
}

impl StreamingDigest for Md5 {
    fn update(&mut self, mut data: &[u8]) {
        while !data.is_empty() {
            let count = (64 - self.offset).min(data.len());
            self.buffer[self.offset..self.offset + count].copy_from_slice(&data[..count]);
            self.offset += count;
            self.length += count as u64;
            data = &data[count..];
            if self.offset == 64 {
                md5_block(&mut self.state, &self.buffer);
                self.offset = 0;
            }
        }
    }

    fn finalize(mut self) -> String {
        let mut tail = vec![0_u8; if self.offset >= 56 { 128 } else { 64 }];
        tail[..self.offset].copy_from_slice(&self.buffer[..self.offset]);
        tail[self.offset] = 0x80;
        let bits = self.length.wrapping_mul(8).to_le_bytes();
        tail[tail.len() - 8..].copy_from_slice(&bits);
        for chunk in tail.chunks_exact(64) {
            md5_block(&mut self.state, chunk);
        }
        hex_le(&self.state)
    }
}

pub(super) struct Sha1 {
    state: [u32; 5],
    buffer: [u8; 64],
    offset: usize,
    length: u64,
}

impl Default for Sha1 {
    fn default() -> Self {
        Self {
            state: [0x67452301, 0xefcdab89, 0x98badcfe, 0x10325476, 0xc3d2e1f0],
            buffer: [0; 64],
            offset: 0,
            length: 0,
        }
    }
}

impl StreamingDigest for Sha1 {
    fn update(&mut self, mut data: &[u8]) {
        while !data.is_empty() {
            let count = (64 - self.offset).min(data.len());
            self.buffer[self.offset..self.offset + count].copy_from_slice(&data[..count]);
            self.offset += count;
            self.length += count as u64;
            data = &data[count..];
            if self.offset == 64 {
                sha1_block(&mut self.state, &self.buffer);
                self.offset = 0;
            }
        }
    }

    fn finalize(mut self) -> String {
        finish_big_endian(&mut self.state, &self.buffer, self.offset, self.length, sha1_block);
        hex_be(&self.state)
    }
}

pub(super) struct Sm3 {
    state: [u32; 8],
    buffer: [u8; 64],
    offset: usize,
    length: u64,
}

impl Default for Sm3 {
    fn default() -> Self {
        Self {
            state: [
                0x7380166f, 0x4914b2b9, 0x172442d7, 0xda8a0600, 0xa96f30bc, 0x163138aa, 0xe38dee4d,
                0xb0fb0e4e,
            ],
            buffer: [0; 64],
            offset: 0,
            length: 0,
        }
    }
}

impl StreamingDigest for Sm3 {
    fn update(&mut self, mut data: &[u8]) {
        while !data.is_empty() {
            let count = (64 - self.offset).min(data.len());
            self.buffer[self.offset..self.offset + count].copy_from_slice(&data[..count]);
            self.offset += count;
            self.length += count as u64;
            data = &data[count..];
            if self.offset == 64 {
                sm3_block(&mut self.state, &self.buffer);
                self.offset = 0;
            }
        }
    }

    fn finalize(mut self) -> String {
        finish_big_endian(&mut self.state, &self.buffer, self.offset, self.length, sm3_block);
        hex_be(&self.state)
    }
}

fn finish_big_endian<const N: usize>(
    state: &mut [u32; N],
    buffer: &[u8; 64],
    offset: usize,
    length: u64,
    compress: fn(&mut [u32; N], &[u8]),
) {
    let mut tail = vec![0_u8; if offset >= 56 { 128 } else { 64 }];
    tail[..offset].copy_from_slice(&buffer[..offset]);
    tail[offset] = 0x80;
    let bits = length.wrapping_mul(8).to_be_bytes();
    tail[tail.len() - 8..].copy_from_slice(&bits);
    for chunk in tail.chunks_exact(64) {
        compress(state, chunk);
    }
}

fn md5_block(state: &mut [u32; 4], block: &[u8]) {
    let mut words = [0_u32; 16];
    for index in 0..16 {
        words[index] = u32::from_le_bytes(block[index * 4..index * 4 + 4].try_into().unwrap());
    }
    let mut a = state[0];
    let mut b = state[1];
    let mut c = state[2];
    let mut d = state[3];
    for index in 0..64 {
        let (value, word) = if index < 16 {
            ((b & c) | (!b & d), index)
        } else if index < 32 {
            ((d & b) | (!d & c), (5 * index + 1) % 16)
        } else if index < 48 {
            (b ^ c ^ d, (3 * index + 5) % 16)
        } else {
            (c ^ (b | !d), (7 * index) % 16)
        };
        let next = b.wrapping_add(
            (a.wrapping_add(value)
                .wrapping_add(MD5_K[index])
                .wrapping_add(words[word]))
            .rotate_left(MD5_SHIFTS[index]),
        );
        a = d;
        d = c;
        c = b;
        b = next;
    }
    state[0] = state[0].wrapping_add(a);
    state[1] = state[1].wrapping_add(b);
    state[2] = state[2].wrapping_add(c);
    state[3] = state[3].wrapping_add(d);
}

fn sha1_block(state: &mut [u32; 5], block: &[u8]) {
    let mut words = [0_u32; 80];
    for index in 0..16 {
        words[index] = u32::from_be_bytes(block[index * 4..index * 4 + 4].try_into().unwrap());
    }
    for index in 16..80 {
        words[index] = (words[index - 3] ^ words[index - 8] ^ words[index - 14] ^ words[index - 16])
            .rotate_left(1);
    }
    let mut a = state[0];
    let mut b = state[1];
    let mut c = state[2];
    let mut d = state[3];
    let mut e = state[4];
    for index in 0..80 {
        let (constant, mix) = if index < 20 {
            (0x5a827999, (b & c) | (!b & d))
        } else if index < 40 {
            (0x6ed9eba1, b ^ c ^ d)
        } else if index < 60 {
            (0x8f1bbcdc, (b & c) | (b & d) | (c & d))
        } else {
            (0xca62c1d6, b ^ c ^ d)
        };
        let next = a
            .rotate_left(5)
            .wrapping_add(mix)
            .wrapping_add(e)
            .wrapping_add(constant)
            .wrapping_add(words[index]);
        e = d;
        d = c;
        c = b.rotate_left(30);
        b = a;
        a = next;
    }
    state[0] = state[0].wrapping_add(a);
    state[1] = state[1].wrapping_add(b);
    state[2] = state[2].wrapping_add(c);
    state[3] = state[3].wrapping_add(d);
    state[4] = state[4].wrapping_add(e);
}

fn sm3_block(state: &mut [u32; 8], block: &[u8]) {
    let mut words = [0_u32; 68];
    for index in 0..16 {
        words[index] = u32::from_be_bytes(block[index * 4..index * 4 + 4].try_into().unwrap());
    }
    for index in 16..68 {
        let value = words[index - 16] ^ words[index - 9] ^ words[index - 3].rotate_left(15);
        words[index] = p1(value) ^ words[index - 13].rotate_left(7) ^ words[index - 6];
    }
    let mut mixed = [0_u32; 64];
    for index in 0..64 {
        mixed[index] = words[index] ^ words[index + 4];
    }
    let mut a = state[0];
    let mut b = state[1];
    let mut c = state[2];
    let mut d = state[3];
    let mut e = state[4];
    let mut f = state[5];
    let mut g = state[6];
    let mut h = state[7];
    for index in 0..64 {
        let constant = if index < 16 { 0x79cc4519 } else { 0x7a879d8a };
        let ss1 = a
            .rotate_left(12)
            .wrapping_add(e)
            .wrapping_add(constant.rotate_left((index % 32) as u32))
            .rotate_left(7);
        let ss2 = ss1 ^ a.rotate_left(12);
        let ff = if index < 16 { a ^ b ^ c } else { (a & b) | (a & c) | (b & c) };
        let gg = if index < 16 { e ^ f ^ g } else { (e & f) | (!e & g) };
        let tt1 = ff.wrapping_add(d).wrapping_add(ss2).wrapping_add(mixed[index]);
        let tt2 = gg.wrapping_add(h).wrapping_add(ss1).wrapping_add(words[index]);
        d = c;
        c = b.rotate_left(9);
        b = a;
        a = tt1;
        h = g;
        g = f.rotate_left(19);
        f = e;
        e = p0(tt2);
    }
    let next = [a, b, c, d, e, f, g, h];
    for index in 0..8 {
        state[index] ^= next[index];
    }
}

fn p0(value: u32) -> u32 {
    value ^ value.rotate_left(9) ^ value.rotate_left(17)
}

fn p1(value: u32) -> u32 {
    value ^ value.rotate_left(15) ^ value.rotate_left(23)
}

fn hex_le(words: &[u32]) -> String {
    words.iter().flat_map(|word| word.to_le_bytes()).map(|byte| format!("{byte:02x}")).collect()
}

fn hex_be(words: &[u32]) -> String {
    words.iter().flat_map(|word| word.to_be_bytes()).map(|byte| format!("{byte:02x}")).collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    fn digest<T: Default + StreamingDigest>(bytes: &[u8]) -> String {
        let mut hasher = T::default();
        hasher.update(bytes);
        hasher.finalize()
    }

    #[test]
    fn matches_the_shared_digest_vectors() {
        let samples: [&[u8]; 4] = [b"", b"abc", b"MooTool", &(0_u8..100).collect::<Vec<_>>()];
        let md5 = [
            "d41d8cd98f00b204e9800998ecf8427e",
            "900150983cd24fb0d6963f7d28e17f72",
            "2341660ec51e960610ee1d84f5568951",
            "7acedd1a84a4cfcb6e7a16003242945e",
        ];
        let sha1 = [
            "da39a3ee5e6b4b0d3255bfef95601890afd80709",
            "a9993e364706816aba3e25717850c26c9cd0d89d",
            "91ce2b4dc75ef65bc81db7ebbbe930b3ec23e1ed",
            "1e6634bfaebc0348298105923d0f26e47aa33ff5",
        ];
        let sm3 = [
            "1ab21d8355cfa17f8e61194831e81a8f22bec8c728fefb747ed035eb5082aa2b",
            "66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0",
            "bfe962397484dae9d193d629dff6f0a04087473c3a020ee6532db9b8313315d0",
            "4b2833c158dd41614b76e37f18889243bd6b4a744e36de60920a2f89e409c64e",
        ];
        for (index, sample) in samples.iter().enumerate() {
            assert_eq!(digest::<Md5>(sample), md5[index]);
            assert_eq!(digest::<Sha1>(sample), sha1[index]);
            assert_eq!(digest::<Sm3>(sample), sm3[index]);
        }
    }
}
