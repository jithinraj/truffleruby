# Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require_relative '../../ruby/spec_helper'

class KeyInfoFixture
  
  def initialize
    @exists = 14
  end

  attr_reader :ro
  attr_accessor :rw
  attr_writer :wo

end

describe "Truffle::Interop.key_info" do
  describe "for a Hash with String keys" do
    
    before :each do
      @hash = {'a' => 1, 'b' => 2, 'c' => 3}
      @hash.instance_variable_set(:@exists, 14)
    end
    
    it "returns :existing for all keys" do
      @hash.keys.each do |k|
        Truffle::Interop.key_info(@hash, k).should include(:existing)
      end
    end
    
    it "returns :readable for all keys" do
      @hash.keys.each do |k|
        Truffle::Interop.key_info(@hash, k).should include(:readable)
      end
    end
    
    it "returns :writable for all keys" do
      @hash.keys.each do |k|
        Truffle::Interop.key_info(@hash, k).should include(:writable)
      end
    end
    
    it "does not return :writable for all keys if the hash is frozen" do
      @hash.freeze
      @hash.keys.each do |k|
        Truffle::Interop.key_info(@hash, k).should_not include(:writable)
      end
    end
    
    it "does not return :invocable for keys" do
      @hash.keys.each do |k|
        Truffle::Interop.key_info(@hash, k).should_not include(:invocable)
      end
    end
    
    it "does not return :internal for keys" do
      @hash.keys.each do |k|
        Truffle::Interop.key_info(@hash, k).should_not include(:internal)
      end
    end
    
    it "returns nothing for a missing key" do
      Truffle::Interop.key_info(@hash, :key_not_in_key_info_hash).should be_empty
    end
    
    it "returns :existing for an instance variable" do
      Truffle::Interop.key_info(@hash, :@exists).should include(:existing)
    end

    it "returns :readable for an instance variable" do
      Truffle::Interop.key_info(@hash, :@exists).should include(:readable)
    end

    it "returns :writable for an instance variable" do
      Truffle::Interop.key_info(@hash, :@exists).should include(:writable)
    end

    it "returns :readable for an instance variable if the hash is frozen" do
      @hash.freeze
      Truffle::Interop.key_info(@hash, :@exists).should include(:readable)
    end

    it "does not return :writable for an instance variable if the hash is frozen" do
      @hash.freeze
      Truffle::Interop.key_info(@hash, :@exists).should_not include(:writable)
    end
    
    it "returns :internal for an instance variable" do
      Truffle::Interop.key_info(@hash, :@exists).should include(:internal)
    end

    it "returns nothing for an instance variable that does not exist" do
      Truffle::Interop.key_info(@hash, :@does_not_exist).should be_empty
    end
  
  end
  
  describe "for a general object" do
    
    before :each do
      @object = KeyInfoFixture.new
    end

    it "returns :existing for all methods" do
      Truffle::Interop.key_info(@object, :ro).should include(:existing)
      Truffle::Interop.key_info(@object, :rw).should include(:existing)
      Truffle::Interop.key_info(@object, :wo).should include(:existing)
    end

    it "returns :readable for a readable name" do
      Truffle::Interop.key_info(@object, :ro).should include(:readable)
      Truffle::Interop.key_info(@object, :rw).should include(:readable)
    end

    it "does not return :readable for a write-only name" do
      Truffle::Interop.key_info(@object, :wo).should_not include(:readable)
    end

    it "returns :writable for a writable name" do
      Truffle::Interop.key_info(@object, :rw).should include(:writable)
      Truffle::Interop.key_info(@object, :wo).should include(:writable)
    end

    it "does not return :writable for a writable name for a frozen hash" do
      @object.freeze
      Truffle::Interop.key_info(@object, :rw).should_not include(:writable)
      Truffle::Interop.key_info(@object, :wo).should_not include(:writable)
    end

    it "does not return :writable for a read-only name" do
      Truffle::Interop.key_info(@object, :ro).should_not include(:writable)
    end

    it "does not return :invocable" do
      Truffle::Interop.key_info(@object, :ro).should_not include(:invocable)
      Truffle::Interop.key_info(@object, :rw).should_not include(:invocable)
      Truffle::Interop.key_info(@object, :wo).should_not include(:invocable)
    end

    it "does not return :internal for methods" do
      Truffle::Interop.key_info(@object, :ro).should_not include(:internal)
      Truffle::Interop.key_info(@object, :rw).should_not include(:internal)
      Truffle::Interop.key_info(@object, :wo).should_not include(:internal)
    end

    it "does not return anything for an undefined method" do
      Truffle::Interop.key_info(@object, :method_not_defined_in_key_info_fixture).should be_empty
    end
    
    it "returns :existing for an instance variable" do
      Truffle::Interop.key_info(@object, :@exists).should include(:existing)
    end
    
    it "returns :readable for an instance variable" do
      Truffle::Interop.key_info(@object, :@exists).should include(:readable)
    end
    
    it "returns :writable for an instance variable" do
      Truffle::Interop.key_info(@object, :@exists).should include(:writable)
    end
    
    it "returns :readable for an instance variable if the object is frozen" do
      @object.freeze
      Truffle::Interop.key_info(@object, :@exists).should include(:readable)
    end
    
    it "does not return :writable for an instance variable if the object is frozen" do
      @object.freeze
      Truffle::Interop.key_info(@object, :@exists).should_not include(:writable)
    end
    
    it "returns :internal for an instance variable" do
      Truffle::Interop.key_info(@object, :@exists).should include(:internal)
    end
    
    it "returns nothing for an instance variable that does not exist" do
      Truffle::Interop.key_info(@object, :@does_not_exist).should be_empty
    end

  end
end
