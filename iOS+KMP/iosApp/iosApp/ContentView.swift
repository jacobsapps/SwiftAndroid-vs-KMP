import SwiftUI

struct ContentView: View {
    @StateObject private var viewModel = RollerCoasterListViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                TextField("Search", text: $viewModel.query)
                    .textFieldStyle(.roundedBorder)
                    .padding(.horizontal)

                if viewModel.isLoading && viewModel.items.isEmpty {
                    ProgressView()
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if viewModel.items.isEmpty {
                    Spacer()
                    Text(viewModel.errorMessage ?? "No results")
                        .foregroundStyle(.secondary)
                    Spacer()
                } else {
                    List(viewModel.items) { item in
                        NavigationLink(value: item) {
                            RollerCoasterRow(item: item)
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationDestination(for: RollerCoasterItem.self) { item in
                RollerCoasterDetailView(item: item)
            }
            .navigationTitle("Roller Coasters")
        }
    }
}

private struct RollerCoasterRow: View {
    let item: RollerCoasterItem

    var body: some View {
        HStack(spacing: 16) {
            if let url = item.imageURL {
                AsyncImage(url: url, transaction: Transaction(animation: .default)) { phase in
                    switch phase {
                    case .empty:
                        ProgressView()
                            .frame(width: 64, height: 64)
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(width: 64, height: 64)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    case .failure:
                        Color.gray.opacity(0.2)
                            .frame(width: 64, height: 64)
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    @unknown default:
                        EmptyView()
                    }
                }
            }
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.headline)
                let count = item.prebuiltDesigns.count
                Text("\(count) roller coaster\(count == 1 ? "" : "s")")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 8)
    }
}

#Preview {
    ContentView()
}
