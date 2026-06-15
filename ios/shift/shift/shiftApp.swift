import SwiftData
import SwiftUI

@main
struct ShiftApp: App {
    @State private var container: AppContainer

    init() {
        do {
            let appContainer = try AppContainer()
            _container = State(initialValue: appContainer)
        } catch {
            fatalError("앱 컨테이너 초기화 실패: \(error)")
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(container)
                .modelContainer(container.modelContainer)
                .task {
                    let weeks = (try? await container.repository.getAllWeeks()) ?? []
                    WidgetSharedStore.write(weeks)
                }
        }
    }
}
