import SwiftUI

struct ContentView: View {
    @Environment(AppContainer.self) private var container

    var body: some View {
        HomeView(
            repository: container.repository,
            preferencesRepository: container.preferencesRepository,
            processImageUseCase: container.processImageUseCase
        )
    }
}
