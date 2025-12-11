import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import composeApp

@main
struct iOSApp: App {
    init() {
        print("Firebase configuring...")
        FirebaseApp.configure()
        print("Setting up bridge...")
        setupFirebaseBridge()
        print("Setup complete")
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private func setupFirebaseBridge() {
        // Auth callbacks
        FirebaseBridgeKt.firebaseGetCurrentUserId = {
            let uid = Auth.auth().currentUser?.uid
            print("getCurrentUserId: \(uid ?? "nil")")
            return uid
        }

        FirebaseBridgeKt.firebaseSignInWithEmail = { email, password, completion in
            print("signInWithEmail: \(email)")
            Auth.auth().signIn(withEmail: email, password: password) { result, error in
                if let error = error {
                    print("Sign in error: \(error.localizedDescription)")
                    completion(nil, error.localizedDescription)
                } else if let user = result?.user {
                    print("Sign in success: \(user.uid)")

                    // Create AuthResult data class
                    let authResult = AuthResult(
                        uid: user.uid,
                        email: user.email,
                        displayName: user.displayName,
                        isEmailVerified: user.isEmailVerified
                    )

                    completion(authResult, nil)
                }
            }
        }

        FirebaseBridgeKt.firebaseCreateUserWithEmail = { email, password, displayName, completion in
            print("createUserWithEmail: \(email)")
            Auth.auth().createUser(withEmail: email, password: password) { result, error in
                if let error = error {
                    print("Create user error: \(error.localizedDescription)")
                    completion(nil, error.localizedDescription)
                    return
                }

                guard let user = result?.user else {
                    completion(nil, "No user created")
                    return
                }

                // Update display name if provided
                if let displayName = displayName {
                    let changeRequest = user.createProfileChangeRequest()
                    changeRequest.displayName = displayName
                    changeRequest.commitChanges { error in
                        if let error = error {
                            print("Error updating profile: \(error)")
                        }
                    }
                }

                print("User created: \(user.uid)")

                //Create AuthResult
                let authResult = AuthResult(
                    uid: user.uid,
                    email: user.email,
                    displayName: displayName ?? user.displayName,
                    isEmailVerified: user.isEmailVerified
                )

                completion(authResult, nil)
            }
        }

        FirebaseBridgeKt.firebaseSendPasswordReset = { email, completion in
            print("sendPasswordReset: \(email)")
            Auth.auth().sendPasswordReset(withEmail: email) { error in
                completion(error?.localizedDescription)
            }
        }

        FirebaseBridgeKt.firebaseSendEmailVerification = { completion in
            guard let user = Auth.auth().currentUser else {
                completion("No user logged in")
                return
            }

            print("sendEmailVerification")
            user.sendEmailVerification { error in
                completion(error?.localizedDescription)
            }
        }

        FirebaseBridgeKt.firebaseSignOut = {
            print("signOut")
            try? Auth.auth().signOut()
        }

        // Firestore callbacks
        FirebaseBridgeKt.firestoreSaveSession = { userId, locationName, latitude, longitude, dataPoints, completion in
            print("saveSession: \(userId), location: \(locationName)")
            let db = Firestore.firestore()

            let sessionData: [String: Any] = [
                "timestamp": Date().timeIntervalSince1970 * 1000,
                "location": locationName,
                "lat": latitude,
                "lon": longitude,
                "dataPoints": dataPoints
            ]

            db.collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .addDocument(data: sessionData) { error in
                    if let error = error {
                        print("Save error: \(error)")
                        // Return result object with success=false
                        let result = FirestoreResult(
                            success: false,
                            documentId: nil,
                            error: error.localizedDescription
                        )
                        completion(result)
                    } else {
                        print("Session saved")
                        // Return result object with success=true
                        let result = FirestoreResult(
                            success: true,  // Bool works in data class!
                            documentId: "success",
                            error: nil
                        )
                        completion(result)
                    }
                }
        }

        FirebaseBridgeKt.firestoreFetchHistory = { userId, sortDescending, startDateMillis, endDateMillis, completion in
            print("fetchHistory: \(userId), descending: \(sortDescending)")
            let db = Firestore.firestore()

            let descending = sortDescending.boolValue

            var query: Query = db.collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .order(by: "timestamp", descending: descending)

            if let start = startDateMillis {
                query = query.whereField("timestamp", isGreaterThanOrEqualTo: start.int64Value)
            }
            if let end = endDateMillis {
                query = query.whereField("timestamp", isLessThanOrEqualTo: end.int64Value)
            }

            query.getDocuments { snapshot, error in
                if let error = error {
                    print("Fetch error: \(error)")
                    completion(nil, error.localizedDescription)
                    return
                }

                let sessions = snapshot?.documents.map { doc -> [String: Any] in
                    var data = doc.data()
                    data["id"] = doc.documentID
                    
                    return data
                } ?? []

                print("Fetched \(sessions.count) sessions")
                completion(sessions, nil)
            }
        }

        FirebaseBridgeKt.firestoreDeleteSession = { userId, sessionId, completion in
            print("deleteSession: \(sessionId)")
            let db = Firestore.firestore()

            db.collection("waveHistory")
                .document(userId)
                .collection("sessions")
                .document(sessionId)
                .delete { error in
                    if let error = error {
                        print("Delete error: \(error)")
                        // Return result object
                        let result = FirestoreResult(
                            success: false,
                            documentId: nil,
                            error: error.localizedDescription
                        )
                        completion(result)
                    } else {
                        print("Session deleted")
                        // Return result object
                        let result = FirestoreResult(
                            success: true,
                            documentId: nil,
                            error: nil
                        )
                        completion(result)
                    }
                }
        }

        print("All callbacks registered")
    }
}
