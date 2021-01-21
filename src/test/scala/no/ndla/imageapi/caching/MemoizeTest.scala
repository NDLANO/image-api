/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.caching

import no.ndla.imageapi.UnitSuite
import org.mockito.Mockito._

class MemoizeTest extends UnitSuite {

  class Target {
    def targetMethod(): String = "Hei"
  }

  test("That an uncached value will do an actual call") {
    val targetMock = mock[Target]
    val memoizedTarget = Memoize[String](Long.MaxValue, targetMock.targetMethod _)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    verify(targetMock, times(1)).targetMethod()
  }

  test("That a cached value will not forward the call to the target") {
    val targetMock = mock[Target]
    val memoizedTarget = Memoize[String](Long.MaxValue, targetMock.targetMethod _)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")
    Seq(1 to 10).foreach(i => {
      memoizedTarget() should equal("Hello from mock")
    })
    verify(targetMock, times(1)).targetMethod()
  }

  test("That the cache is invalidated after cacheMaxAge") {
    val cacheMaxAgeInMs = 20
    val targetMock = mock[Target]
    val memoizedTarget = Memoize[String](cacheMaxAgeInMs, targetMock.targetMethod _)

    when(targetMock.targetMethod()).thenReturn("Hello from mock")

    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")
    Thread.sleep(cacheMaxAgeInMs)
    memoizedTarget() should equal("Hello from mock")
    memoizedTarget() should equal("Hello from mock")

    verify(targetMock, times(2)).targetMethod()
  }
}
