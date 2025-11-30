package dev.oblac.eddi.example.college

import dev.oblac.eddi.CommandError

// exercising the sealed interface feature
// to group all application-specific command errors
// todo maybe do the same with commands? events not as we dont use switches on them
sealed interface AppCommandError : CommandError

data class StudentAlreadyRegistered(val email: String) : AppCommandError
