package com.grab.test.di

import javax.inject.Inject

class SomeDependency @Inject constructor() {
  fun doSomething() {
    println("Doing something")
  }
}
