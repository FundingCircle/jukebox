# frozen_string_literal: true

require 'json'
require 'jukebox/core_ext/hash'

describe 'Hash#dig_in' do
  it 'sets a nested value' do
    expect({ a: { b: { c: :foo } } }.dig_in(:a, :b, :c, :bar))
      .to eq(a: { b: { c: :bar } })
  end
end

describe 'Hash#transmittable_as' do
  it '' do
    foo = Unserializable.new

    expect({ a: { b: 1, c: foo } }.transmittable_as(&JSON.method(:generate)))
      .to eq([{ a: { b: 1 } }, { a: { c: foo } }])
  end
end
