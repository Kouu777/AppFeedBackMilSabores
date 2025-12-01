package com.example.proyectomilsabores.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.proyectomilsabores.adapters.ReviewsAdapter
import com.example.proyectomilsabores.databinding.FragmentReviewsBinding
import com.example.proyectomilsabores.models.Review
import com.example.proyectomilsabores.data.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReviewsFragment : Fragment() {

    private var _binding: FragmentReviewsBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var userRepository: UserRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReviewsBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        userRepository = UserRepository(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadReviewsFromFirestore()
    }

    override fun onResume() {
        super.onResume()
        // Recargar reseñas para mostrar nuevas que se hayan creado
        loadReviewsFromFirestore()
    }

    private fun setupRecyclerView() {
        binding.reviewsRecyclerView.layoutManager = LinearLayoutManager(context)
        binding.reviewsRecyclerView.adapter = ReviewsAdapter(emptyList())
    }

    private fun loadReviewsFromFirestore() {
        // email del usuario guardado en SharedPreferences (autenticación local)
        val userEmail = userRepository.getUserId() // Este contiene el email

        Log.d("ReviewsFragment", "Email del usuario: $userEmail")

        if (userEmail.isNullOrEmpty()) {
            Log.e("ReviewsFragment", "No se encontró el email del usuario en SharedPreferences")
            Toast.makeText(context, "No se pudo identificar al usuario. Por favor, inicia sesión de nuevo.", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("reviews")
            .whereEqualTo("userEmail", userEmail)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("ReviewsFragment", "No hay reseñas para el usuario: $userEmail")
                    Toast.makeText(context, "Aún no has escrito ninguna reseña.", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val reviewsList = documents.map { doc ->
                    // convertimos cada documento de Firestore a objeto de datos Review
                    Review(
                        title = doc.getString("productName") ?: "Sin Título",
                        rating = (doc.getDouble("rating") ?: 0.0).toFloat(),
                        comment = doc.getString("comment") ?: ""
                    )
                }

                Log.d("ReviewsFragment", "Reseñas cargadas: ${reviewsList.size}")
                // Actualizamos el adaptador del RecyclerView con los datos reales
                binding.reviewsRecyclerView.adapter = ReviewsAdapter(reviewsList)
            }
            .addOnFailureListener { exception ->
                Log.e("ReviewsFragment", "Error al cargar las reseñas desde Firestore", exception)
                Toast.makeText(context, "Error al cargar tus reseñas: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}