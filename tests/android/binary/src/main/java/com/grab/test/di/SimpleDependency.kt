package com.grab.test.di

import javax.inject.Inject

class SimpleDependency @Inject constructor() {
  fun doSomething() {
    println("Doing something")
  }
}
