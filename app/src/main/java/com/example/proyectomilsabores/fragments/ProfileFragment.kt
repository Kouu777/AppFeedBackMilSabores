package com.example.proyectomilsabores.fragments

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.proyectomilsabores.MainLogin
import com.example.proyectomilsabores.databinding.FragmentProfileBinding
import com.example.proyectomilsabores.data.UserRepository
import com.example.proyectomilsabores.DbLog.DbHelper

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private lateinit var userRepository: UserRepository
    private lateinit var dbHelper: DbHelper
    private var isEditMode = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        userRepository = UserRepository(requireContext())
        dbHelper = DbHelper(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserData()
        updateUIMode()


        binding.buttonEditProfile.setOnClickListener {
            setEditMode(true)
        }


        binding.buttonCancel.setOnClickListener {
            setEditMode(false)
            loadUserData()
        }


        binding.buttonSaveChanges.setOnClickListener {
            saveUserData()
        }


        binding.buttonLogout.setOnClickListener {
            userRepository.clearUserData()
            val intent = Intent(requireActivity(), MainLogin::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun loadUserData() {
        // email del usuario guardado en SharedPreferences
        val userEmail = userRepository.getUserId()
        val userName = userRepository.getUserName()

        if (!userEmail.isNullOrEmpty()) {
            // usuario de la base de datos local
            val user = dbHelper.getUserByEmail(userEmail)
            if (user != null) {
                // Mostrar en vista de lectura
                binding.textViewUserName.text = user.nombre
                binding.textViewUserEmail.text = user.email
                // Cargar en los campos de edición
                binding.editTextName.setText(user.nombre)
                binding.editTextEmail.setText(user.email)
            } else {
                binding.textViewUserName.text = userName ?: "Usuario"
                binding.textViewUserEmail.text = userEmail
                binding.editTextName.setText(userName ?: "")
                binding.editTextEmail.setText(userEmail)
            }
        } else {
            binding.textViewUserName.text = "Usuario"
            binding.textViewUserEmail.text = "sin-email@ejemplo.com"
            binding.editTextName.setText("")
            binding.editTextEmail.setText("")
        }
    }

    private fun setEditMode(enabled: Boolean) {
        isEditMode = enabled
        updateUIMode()
    }

    private fun updateUIMode() {
        if (isEditMode) {
            // formulario de edición
            binding.profileCard.visibility = View.GONE
            binding.buttonEditProfile.visibility = View.GONE
            binding.editFormCard.visibility = View.VISIBLE
            binding.buttonSaveChanges.visibility = View.VISIBLE
            binding.buttonCancel.visibility = View.VISIBLE
        } else {
            // vista de lectura
            binding.profileCard.visibility = View.VISIBLE
            binding.buttonEditProfile.visibility = View.VISIBLE
            binding.editFormCard.visibility = View.GONE
            binding.buttonSaveChanges.visibility = View.GONE
            binding.buttonCancel.visibility = View.GONE
        }
    }

    private fun saveUserData() {
        val newName = binding.editTextName.text.toString().trim()
        val newEmail = binding.editTextEmail.text.toString().trim()

        if (newName.isEmpty()) {
            Toast.makeText(context, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        if (newEmail.isEmpty()) {
            Toast.makeText(context, "El email no puede estar vacío", Toast.LENGTH_SHORT).show()
            return
        }

        // email actual del usuario
        val currentEmail = userRepository.getUserId()

        if (currentEmail.isNullOrEmpty()) {
            Toast.makeText(context, "Error: No se encontró el usuario actual", Toast.LENGTH_SHORT).show()
            return
        }

        // actualizar en la base de datos local
        val updateSuccess = dbHelper.updateUser(currentEmail, newName, newEmail)

        if (updateSuccess) {
            // actualizar en SharedPreferences
            userRepository.saveUserName(newName)
            userRepository.saveUserId(newEmail)

            Toast.makeText(context, "Datos actualizados correctamente", Toast.LENGTH_SHORT).show()

            // volver a modo lectura
            setEditMode(false)
            loadUserData()
        } else {
            Toast.makeText(context, "Error al actualizar los datos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
