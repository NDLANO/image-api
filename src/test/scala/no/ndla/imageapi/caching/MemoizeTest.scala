/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.caching

import no.ndla.imageapi.UnitSuite
import org.mockito.Mockito._
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

class MemoizeTest extends UnitSuite {

  class Target {
    def targetMethod(): String = "Hei"
  }

  test("That an uncached value will do an actual call") {
    val targetMock = mock[Target]
    val memoizedTarget = Memoize[String](Long.MaxValue, targetMock.targetMethod)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    verify(targetMock, times(1)).targetMethod()
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock = mock[Target]
    val memoizedTarget = Memoize[String](Long.MaxValue, targetMock.targetMethod)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    Seq(1 to 10).foreach (i => {
      memoizedTarget() should equal("Hello from mock")
    })
    verify(targetMock, times(1)).targetMethod()
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 500
    val targetMock = mock[Target]
    val memoizedTarget = Memoize[String](cacheMaxAgeInMs, targetMock.targetMethod)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")

    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    Thread.sleep(500)
    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")

    verify(targetMock, times(2)).targetMethod()
  }

  /* TODO: Does not work
  test("That multiple threads only is able to reach target once") {
    val targetMock = mock[Target]
    val memoizedTarget = Memoize[String](Long.MaxValue, targetMock.targetMethod)

    when(targetMock.targetMethod()).thenAnswer(new Answer[String]{
      override def answer(invocation: InvocationOnMock): String = {
        Thread.sleep(100)
        "Hello from mock"
      }
    })

    val runnable = new Runnable { override def run(): Unit = memoizedTarget() should equal ("Hello from mock")}
    val t1 = new Thread(runnable)
    val t2 = new Thread(runnable)

    t1.start()
    t2.start()

    verify(targetMock, times(1)).targetMethod()
  }
  */
}
