//
// Created by Sativa Maciel` on 12/7/25.
//
import Foundation
import FirebaseAuth
import FirebaseFirestore

@objc public class FirebaseBridge: NSObject {

    // MARK: - Auth Methods

    @objc public static func getCurrentUserId() -> String? {
        return Auth.auth().currentUser?.uid
    }

    @objc public static func signInWithEmail(
        _ email: String,
        _ password: String,
        completion: @escaping (String?, String?, String?, Bool, String?) -> Void
    ) {
        Auth.auth().signIn(withEmail: email, password: password) { result, error in
            if let error = error {
                completion(nil, nil, nil, false, error.localizedDescription)
            } else if let user = result?.user {
                completion(
                    user.uid,
                    user.email,
                    user.displayName,
                    user.isEmailVerified,
                    nil
                )
            }
        }
    }

    @objc public static func createUserWithEmail(
        _ email: String,
        _ password: String,
        _ displayName: String?,
        completion: @escaping (String?, String?, String?, Bool, String?) -> Void
    ) {
        Auth.auth().createUser(withEmail: email, password: password) { result, error in
            if let error = error {
                completion(nil, nil, nil, false, error.localizedDescription)
                return
            }

            guard let user = result?.user else {
                completion(nil, nil, nil, false, "No user created")
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

            completion(
                user.uid,
                user.email,
                displayName ?? user.displayName,
                user.isEmailVerified,
                nil
            )
        }
    }

    @objc public static func sendPasswordResetEmail(
        _ email: String,
        completion: @escaping (String?) -> Void
    ) {
        Auth.auth().sendPasswordReset(withEmail: email) { error in
            completion(error?.localizedDescription)
        }
    }

    @objc public static func sendEmailVerification(
        completion: @escaping (String?) -> Void
    ) {
        guard let user = Auth.auth().currentUser else {
            completion("No user logged in")
            return
        }

        user.sendEmailVerification { error in
            completion(error?.localizedDescription)
        }
    }

    @objc public static func signOut() {
        try? Auth.auth().signOut()
    }

    // MARK: - Firestore Methods

    @objc public static func saveWaveSession(
        userId: String,
        locationName: String,
        latitude: Double,
        longitude: Double,
        dataPoints: [[String: Any]],
        completion: @escaping (String?, String?) -> Void
    ) {
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
                    completion(nil, error.localizedDescription)
                } else {
                    // Return document ID on success
                    completion("success", nil)
                }
            }
    }

    @objc public static func fetchWaveHistory(
        userId: String,
        sortDescending: Bool,
        startDateMillis: NSNumber?,
        endDateMillis: NSNumber?,
        completion: @escaping ([[String: Any]]?, String?) -> Void
    ) {
        let db = Firestore.firestore()

        var query: Query = db.collection("waveHistory")
            .document(userId)
            .collection("sessions")
            .order(by: "timestamp", descending: sortDescending)

        // Apply date filters
        if let start = startDateMillis {
            query = query.whereField("timestamp", isGreaterThanOrEqualTo: start)
        }
        if let end = endDateMillis {
            query = query.whereField("timestamp", isLessThanOrEqualTo: end)
        }

        query.getDocuments { snapshot, error in
            if let error = error {
                completion(nil, error.localizedDescription)
                return
            }

            let sessions = snapshot?.documents.map { doc -> [String: Any] in
                var data = doc.data()
                data["id"] = doc.documentID
                return data
            } ?? []

            completion(sessions, nil)
        }
    }

    @objc public static func deleteWaveSession(
        _ userId: String,
        _ sessionId: String,
        completion: @escaping (Bool, String?) -> Void
    ) {
        let db = Firestore.firestore()

        db.collection("waveHistory")
            .document(userId)
            .collection("sessions")
            .document(sessionId)
            .delete { error in
                if let error = error {
                    completion(false, error.localizedDescription)
                } else {
                    completion(true, nil)
                }
            }
    }
}