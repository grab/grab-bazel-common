package com.grab.test.di

import dagger.Component

@Component
interface LibraryActivityComponent {
  fun someDependency(): SomeDependency

  @Component.Factory
  interface Factory {
    fun create(): LibraryActivityComponent
  }
}
