package com.grab.test.di

import dagger.Component

@Component
interface TestActivityComponent {

  fun simpleDependency(): SimpleDependency

  @Component.Factory
  interface Factory {
    fun create(): TestActivityComponent
  }
}
