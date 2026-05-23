import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Read Strava credentials from Info.plist.
        // Add STRAVA_CLIENT_ID and STRAVA_CLIENT_SECRET keys there — never hardcode here.
        let plist = Bundle.main.infoDictionary
        let clientId     = plist?["STRAVA_CLIENT_ID"]     as? String ?? ""
        let clientSecret = plist?["STRAVA_CLIENT_SECRET"] as? String ?? ""
        IosKoinSetupKt.doInitKoin(stravaClientId: clientId, stravaClientSecret: clientSecret)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Handle Strava OAuth callback: paceup://localhost/callback?code=...
                    guard url.scheme == "paceup",
                          url.host   == "localhost",
                          let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
                          let code = components.queryItems?.first(where: { $0.name == "code" })?.value
                    else { return }
                    StravaOAuthCodeStore.shared.submitCode(code: code)
                }
        }
    }
}