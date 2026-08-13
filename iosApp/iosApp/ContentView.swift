import SwiftUI
import UIKit
import Shared

/// The entire iOS app is one Compose view controller; SwiftUI is just the front door.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all) // Compose handles its own safe-drawing padding
    }
}
