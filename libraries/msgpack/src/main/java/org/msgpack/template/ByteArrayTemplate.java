//
// MessagePack for Java
//
// Copyright (C) 2009 - 2013 FURUHASHI Sadayuki
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package org.msgpack.template;

import org.msgpack.MessageTypeException;
import org.msgpack.packer.Packer;
import org.msgpack.unpacker.Unpacker;

import java.io.IOException;

public class ByteArrayTemplate extends AbstractTemplate<byte[]> {
    private ByteArrayTemplate() {
    }

    public void write(Packer pk, byte[] target, boolean required)
            throws IOException {
        if (target == null) {
            if (required) {
                throw new MessageTypeException("Attempted to write null");
            }
            pk.writeNil();
            return;
        }
        pk.write(target);
    }

    public byte[] read(Unpacker u, byte[] to, boolean required)
            throws IOException {
        if (!required && u.trySkipNil()) {
            return null;
        }
        return u.readByteArray(); // TODO read to 'to' obj
    }

    static public ByteArrayTemplate getInstance() {
        return instance;
    }

    static final ByteArrayTemplate instance = new ByteArrayTemplate();
}
