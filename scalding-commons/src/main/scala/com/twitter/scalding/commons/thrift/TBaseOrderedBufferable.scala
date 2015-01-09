package com.twitter.scalding.commons.thrift

import com.twitter.scalding.typed.OrderedBufferable
import cascading.tuple.hadoop.io.BufferedInputStream
import cascading.tuple.StreamComparator
import org.apache.thrift.protocol.TCompactProtocol
import org.apache.thrift.protocol.TBinaryProtocol
import org.apache.thrift.TBase
import java.nio.ByteBuffer
import com.twitter.bijection.Inversion.attempt
import com.twitter.bijection.Bufferable
import org.apache.thrift.transport.TIOStreamTransport

abstract class TBaseOrderedBufferable[T <: TBase[_, _]] extends TProtocolOrderedBufferable[T] {

  @transient protected def prototype: T

  /*
    TODO: It would be great if the binary comparasion matched in the in memory for both TBase and ThriftStruct.
    In TBase the limitation is that the TProtocol can't tell a Union vs a Struct apart, and those compare differently deserialized
    */
  def compare(a: T, b: T): Int

  def get(from: java.nio.ByteBuffer): scala.util.Try[(java.nio.ByteBuffer, T)] = attempt(from) { bb =>
    val obj = prototype.deepCopy
    val stream = new com.esotericsoftware.kryo.io.ByteBufferInputStream(bb)
    val len = bb.getInt
    obj.read(factory.getProtocol(new TIOStreamTransport(stream)))
    (bb, obj.asInstanceOf[T])
  }

  def put(bb: java.nio.ByteBuffer, t: T): java.nio.ByteBuffer = {
    val initialPos = bb.position
    val baos = new com.esotericsoftware.kryo.io.ByteBufferOutputStream(bb)
    bb.putInt(0)
    t.write(factory.getProtocol(new TIOStreamTransport(baos)))
    val endPosition = bb.position

    bb.position(initialPos)
    bb.putInt(endPosition - (initialPos + 4))
    bb.position(endPosition)
    bb
  }
}