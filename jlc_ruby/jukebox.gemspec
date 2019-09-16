# frozen_string_literal: true

Gem::Specification.new do |spec|
  spec.name = 'jukebox'
  spec.version = '1.0.5'
  spec.date = '2019-08-28'
  spec.summary = 'A polyglot BDD framework'
  spec.description = 'A polyglot BDD framework'
  spec.authors = ['Matthias Margush']
  spec.executables << 'jlc_ruby'
  spec.files = ['lib/jukebox.rb']
  spec.homepage = 'https://github.com/fundingcircle/jukebox/'
  spec.license = 'BSD-3-clause'
  spec.add_runtime_dependency 'activesupport'
  spec.add_runtime_dependency 'msgpack'
  spec.add_runtime_dependency 'rspec'
end
