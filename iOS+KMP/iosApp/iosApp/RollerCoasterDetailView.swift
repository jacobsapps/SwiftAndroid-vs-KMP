import SwiftUI

struct RollerCoasterDetailView: View {
    let item: RollerCoasterItem

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                if let url = item.imageURL {
                    AsyncImage(url: url, transaction: Transaction(animation: .easeInOut)) { phase in
                        switch phase {
                        case .empty:
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        case .success(let image):
                            image
                                .resizable()
                                .scaledToFit()
                                .frame(maxWidth: .infinity)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        case .failure:
                            Color.gray.opacity(0.2)
                                .frame(height: 200)
                                .frame(maxWidth: .infinity)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                        @unknown default:
                            EmptyView()
                        }
                    }
                }

                Text(item.name)
                    .font(.title2)
                    .bold()

                Text(item.construction)
                    .font(.body)

                if !item.prebuiltDesigns.isEmpty {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Prebuilt Designs")
                            .font(.headline)
                        ForEach(item.prebuiltDesigns, id: \.self) { design in
                            Text("• \(design)")
                                .font(.body)
                        }
                    }
                }

                if let url = URL(string: item.sourceUrl) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Source:")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                        Link(item.sourceUrl, destination: url)
                            .font(.footnote.weight(.medium))
                            .multilineTextAlignment(.leading)
                    }
                } else {
                    Text("Source: \(item.sourceUrl)")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                        .multilineTextAlignment(.leading)
                }
            }
            .padding()
        }
        .navigationTitle(item.name)
        .navigationBarTitleDisplayMode(.inline)
    }
}
