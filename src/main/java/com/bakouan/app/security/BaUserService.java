package com.bakouan.app.security;


import com.bakouan.app.dto.*;
import com.bakouan.app.enums.EAction;
import com.bakouan.app.enums.EStatut;
import com.bakouan.app.mapper.YtMapper;
import com.bakouan.app.model.BaProfil;
import com.bakouan.app.model.BaRole;
import com.bakouan.app.model.BaUser;
import com.bakouan.app.repositories.BaProfilRepository;
import com.bakouan.app.repositories.BaRoleRepository;
import com.bakouan.app.repositories.BaUserRepository;
import com.bakouan.app.service.BaLogService;
import com.bakouan.app.service.BaMailService;
import com.bakouan.app.utils.BaUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Transactional
@Service
@Slf4j
public class BaUserService {

    private final IUserDetailsFacade userDetailsFacade;
    private final UserDetailsService userDetailsService;
    private final BaUserRepository userRepository;
    private final BaProfilRepository profilRepository;
    private final BaRoleRepository roleRepository;
    private final YtMapper mapper = Mappers.getMapper(YtMapper.class);
    private final PasswordEncoder passwordEncoder;
    private final BaLogService logService;
    private final BaMailService mailService;


    /**
     * Avoir la liste des utilisateurs.
     *
     * @return liste des utilisateurs.
     */
    public List<BaUserDto> fetchUtilisateurs() {
        logService.log(new BaLogDto(EAction.V, "Utilisateurs"));

        return this.userRepository.fetchMulticrites(
                        EStatut.A.name(), "", "", "")
                .map(mapper::maps)
                .collect(Collectors.toList());
    }


    /**
     * Fonction de création d'un utilisateur.
     *
     * @param uDto
     * @return user
     */
    public BaUserDto createUser(final BaUserDto uDto) {
        log.info("Création d'un compte utilisateur.");
        logService.log(new BaLogDto(EAction.C, "Utilisateurs : " + uDto.getUsername()));
        // Assigner l'email comme nom d'utilisateur
        uDto.setUsername(uDto.getEmail());

        if (this.userRepository.existsByUsernameIgnoreCase(uDto.getUsername())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom d'utilisateur est déjà occupé.");
        }

        if (this.userRepository.existsByTelephoneIgnoreCase(uDto.getTelephone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de téléphone est déjà utilisé.");
        }

        if (uDto.getEmail() != null && this.userRepository.existsByEmailIgnoreCase(uDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'email est déjà utilisé.");
        }

        if (uDto.getCredentials() == null || BaUtils.isEmpty(uDto.getCredentials().getPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vous devez fournir le mot de passe");
     }

        BaUser user = this.mapper.maps(uDto);
        user.setId(BaUtils.randomUUID());
        String userPassword = uDto.getCredentials().getPassword();
        user.setPassword(this.passwordEncoder.encode(userPassword));
        user.setResetKey(null);
        user.setResetDate(null);
        user.setActivated(Boolean.TRUE);


        BaUser save = this.userRepository.save(user);

        mailService.sendMessage(user.getEmail(), user.getNom() + " " + user.getPrenom(), "Merci " +
                " d'être immatriculé. Votre compte est actif.","Identifiant  de connexion");
        return this.mapper.maps(save);
    }

    public BaUserDto register(final BaRegisterDto registerDto) {
        log.info("Auto-inscription utilisateur: {}", registerDto.getEmail());
        if (!Objects.equals(registerDto.getPassword(), registerDto.getConfirmation())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La confirmation ne correspond pas au mot de passe.");
        }
        String email = registerDto.getEmail().trim().toLowerCase();
        if (this.userRepository.existsByUsernameIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cet email est deja utilise comme identifiant.");
        }
        if (this.userRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'email est deja utilise.");
        }

        BaUser user = new BaUser();
        user.setId(BaUtils.randomUUID());
        user.setUsername(email);
        user.setEmail(email);
        user.setNom(registerDto.getNom());
        user.setPrenom(registerDto.getPrenom());
        user.setPassword(this.passwordEncoder.encode(registerDto.getPassword()));
        user.setTelephone(email);
        user.setLocked(Boolean.FALSE);
        user.setActivated(Boolean.TRUE);
        user.setProfil(resolveDefaultSelfRegistrationProfile());
        return this.mapper.maps(this.userRepository.save(user));
    }

    private BaProfil resolveDefaultSelfRegistrationProfile() {
        BaRole connectRole = roleRepository.findAll().stream()
                .filter(role -> BaRolesConstants.BA_CONNECT.equals(role.getCode()))
                .findFirst()
                .orElseGet(() -> {
                    BaRole role = new BaRole();
                    role.setId(BaUtils.randomUUID());
                    role.setCode(BaRolesConstants.BA_CONNECT);
                    role.setLibelle("Utilisateur connecte");
                    return roleRepository.save(role);
                });
        Optional<BaProfil> existingProfile = profilRepository.findByStatutOrderByCreatedDateDesc(EStatut.A).stream()
                .filter(profil -> "AUTO_INSCRIPTION".equalsIgnoreCase(profil.getLibelle()))
                .findFirst();
        if (existingProfile.isPresent()) {
            BaProfil profil = existingProfile.get();
            if (profil.getRoles().stream().noneMatch(role -> BaRolesConstants.BA_CONNECT.equals(role.getCode()))) {
                profil.getRoles().add(connectRole);
                return profilRepository.save(profil);
            }
            return profil;
        }
        return profilRepository.save(createSelfRegistrationProfile(connectRole));
    }

    private BaProfil createSelfRegistrationProfile(BaRole connectRole) {
                    BaProfil profil = new BaProfil();
                    profil.setId(BaUtils.randomUUID());
                    profil.setLibelle("AUTO_INSCRIPTION");
                    profil.setDescription("Profil par defaut des comptes crees depuis le formulaire public");
                    profil.getRoles().add(connectRole);
                    return profil;
    }


    /**
     * Fonction de mise à jour d'un utilisateur.
     *
     * @param id   l'identifiant de l'utilisateur
     * @param uDto DTO Utilisateur
     */
    public void updateUser(final String id, final BaUserDto uDto) {
        log.info("Met à jour les informations d'un compte.");
        logService.log(new BaLogDto(EAction.U, "Utilisateurs : " + uDto.getUsername()));

        if (id == null || !this.userRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur est introuvable");
        }

        this.userRepository.findById(id)
                .ifPresent(u -> {
                    if (EStatut.D.equals(u.getStatut())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'utilisateur est introuvable");
                    }
                    uDto.setPassword(u.getPassword());
                    uDto.setActivated(u.getActivated());
                    uDto.setResetDate(u.getResetDate());
                    uDto.setResetKey(u.getResetKey());
                    uDto.setLocked(u.getLocked());
                });

        if (this.userRepository.checkDuplicateTelephone(id, uDto.getTelephone())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le numéro de téléphone est déjà utilisé.");
        }

        if (this.userRepository.checkDuplicateEmail(id, uDto.getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'email de téléphone est déjà utilisé.");
        }

        BaUser user = this.mapper.maps(uDto);
        user = this.userRepository.save(user);
        this.mapper.maps(user);
    }

    /**
     * Supprimer logiquement l'utilisateur.
     *
     * @param id identifiant de l'utilisateur
     */
    public void doDeleteUser(final String id) {
        logService.log(new BaLogDto(EAction.D, "Utilisateurs : " + id));

        log.info("Supprime un compte utilisateur. " + id);
        // À partir de là, on est sur de l'existence de l'élément,
        // donc inutile d'utiliser un optional, encore.
        this.userRepository.findById(id)
                .ifPresent(u -> {
                    u.setStatut(EStatut.D);
                    this.userRepository.save(u);
                });
    }


    /**
     * Modifier le mot de passe de l'utilisateur actuellement connecté.
     *
     * @param pwDto DTO du mise à jour du mot de passe
     */
    public void changePassword(final BaUpdatePasswordDto pwDto) {
        log.info("Changement d'identifiant de connexion.");
        logService.log(new BaLogDto(EAction.U, "Reinitialise le mot de passe"));

        // À partir de là, on est sur de l'existence de l'élément,
        // donc inutile d'utiliser un optional, encore.
        final BaUser ylUser = this.userRepository.findOneByUsernameIgnoreCaseAndStatut(
                        userDetailsFacade.getUserDetails().getUsername(), EStatut.A)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "L'utilisateur est introuvable"));
        if (!Objects.equals(pwDto.getConfirmer(), pwDto.getNouveau())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La confirmation ne correspond pas au mot de passe.");
        }
        final boolean matches = this.passwordEncoder.matches(pwDto.getAncien(), ylUser.getPassword());
        if (!matches) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre ancien mot de passe est incorrect");
        }
        ylUser.setPassword(this.passwordEncoder.encode(pwDto.getNouveau()));
        this.userRepository.save(ylUser);
    }


    /**
     * Vérifie si un utilisateur est activé ou pas.
     *
     * @param username Nom d'utilisateur
     * @return un boolean
     */
    public Boolean isActivated(final String username) {
        log.info("Vérification de l'état du compte " + username + " à la connexion.");
        final BaUser ylUser = this.userRepository.findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "L'utilisateur n'existe pas."));
        return ylUser.getActivated();
    }

    /**
     * Recuperer le DTO de l'utilisateur connecte.
     *
     * @return le DTO ou BadRequest.
     */
    public BaUserDto getUserInfoWithMoreDetails() {
        Optional<BaUserDto> ou = this.getCurrentUser();
        return ou.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Utilisateur non connecté."));
    }

    /**
     * Avoir la liste des roles.
     *
     * @return Liste des roles
     */
    public List<BaRoleDto> fetchRoles() {
        log.info("Recupère la liste des rôles.");
        logService.log(new BaLogDto(EAction.V, "Roles"));

        final List<BaRole> allRoles = roleRepository.findAll();
        final BaUserDto userInfoWithMoreDetails = this.getUserInfoWithMoreDetails();
        log.debug("Exclure les rôles liés à l'administrateur");
        // Exclure les rôles liés à l'administrateur.
        return allRoles.stream().parallel()
                .filter(Objects::nonNull)
                .map(mapper::maps)
                .collect(Collectors.toList());
    }

    /**
     * Pour ajouter un role.
     *
     * @param roleDto : le role à ajouter
     * @return BaRoleDto
     */
    public BaRoleDto addRole(final BaRoleDto roleDto) {
        logService.log(new BaLogDto(EAction.C, "Roles : " + roleDto.getLibelle()));
        BaRole role = mapper.maps(roleDto);
        log.info("Ajoute un nouveau role.");
        return mapper.maps(roleRepository.save(role));
    }

    /**
     * Mettre à jour un role.
     *
     * @param pDto : Les modifications du role
     * @return Le role mis à jour
     */
    public BaRoleDto updateRole(final BaRoleDto pDto) {
        logService.log(new BaLogDto(EAction.U, "Roles : " + pDto.getLibelle()));

        BaRole role;
        if (roleRepository.existsById(pDto.getId())) {
            role = roleRepository.getReferenceById(pDto.getId());
            role.setLibelle(pDto.getLibelle());
            roleRepository.save(role);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle introuvable");
        }
        return mapper.maps(roleRepository.save(role));
    }


    /**
     * Recupérer la liste des profils.
     *
     * @return Liste de ProfilDto: une liste de Dto
     */
    public List<BaProfilDto> fetchProfils() {
        log.info("Recupère les profils");
        logService.log(new BaLogDto(EAction.V, "Profils"));

        List<BaProfilDto> datas;
        datas = profilRepository
                .findByStatutOrderByCreatedDateDesc(EStatut.A)
                .stream()
                .map(mapper::maps)
                .collect(Collectors.toList());
        return datas.stream()
                .collect(Collectors.toList());
    }

    /**
     * Ajouter un profil.
     *
     * @param profilDto : Le profil à ajouter
     * @return ProfilDto
     */
    public BaProfilDto addProfil(final BaProfilDto profilDto) {
        profilDto.setId(BaUtils.randomUUID());
        logService.log(new BaLogDto(EAction.C, "Profils" + profilDto.getLibelle()));

        log.info("Crée un nouveau profil " + profilDto.getLibelle());
        return mapper.maps(profilRepository.save(mapper.maps(profilDto)));
    }

    /**
     * Mettre à jour un profil.
     *
     * @param pDto : Les modifications du profil
     * @return ProfilDto: Le profil mis à jour
     */
    public BaProfilDto updateProfil(final BaProfilDto pDto) {
        log.info("Met à jour le profil " + pDto.getLibelle());
        logService.log(new BaLogDto(EAction.U, "Profils" + pDto.getLibelle()));

        BaProfil profil;
        if (profilRepository.existsById(pDto.getId())) {
            profil = profilRepository.getReferenceById(pDto.getId());
            profil.setRoles(pDto.getRoles()
                    .stream().map(mapper::maps)
                    .collect(Collectors.toSet()));
            profil.setLibelle(pDto.getLibelle());
            profil.setDescription(pDto.getDescription());
            profilRepository.save(profil);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "BaProfil introuvable");
        }
        return mapper.maps(profilRepository.save(profil));
    }

    /**
     * Supprimer un profil.
     *
     * @param uuid : Le profil à supprimer
     */
    public void deleteProfil(final String uuid) {
        log.warn("Suppression du profil : {}", uuid);
        logService.log(new BaLogDto(EAction.D, "Profils" + uuid));

        if (profilRepository.existsById(uuid)) {
            profilRepository.findById(uuid)
                    .ifPresent(profil -> {
                        profil.setStatut(EStatut.D);
                        this.profilRepository.save(profil);
                    });
        }
    }

    /**
     * Supprimer un rôle.
     *
     * @param uuid : Le role à supprimer
     */
    public void deleteRole(final String uuid) {
        log.info("Suppression du role : {}", uuid);
        logService.log(new BaLogDto(EAction.D, "Roles" + uuid));

        if (roleRepository.existsById(uuid)) {
            roleRepository.deleteById(uuid);
        }
    }


    /**
     * Récupérer l'utilisateur connecte.
     *
     * @return Un optional
     */
    private Optional<BaUserDto> getCurrentUser() {
        final UserDetails userDetails = userDetailsFacade.getUserDetails();
        if (userDetails != null && userDetails.getUsername() != null) {
            return getOneByUsername(userDetails.getUsername());
        } else {
            return Optional.empty();
        }
    }

    /**
     * Recupere un utilisateur par son nom d'utilisateur.
     *
     * @param username
     * @return un optional de l'utilisateur
     */
    public Optional<BaUserDto> getOneByUsername(final String username) {
        final Optional<BaUser> opt = this.userRepository
                .findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A);
        return opt.map(entity -> {
            final BaUserDto dto = mapper.maps(entity);
            dto.setRoles(this.profilRepository.getReferenceById(dto.getIdProfil()).getRoles()
                    .stream().map(mapper::maps).collect(Collectors.toSet()));
            return dto;
        });
    }

    /**
     * Réinitialiser le mot de passe d'un utilisateur.
     * Juste vérifier que la clé d'activation est correcte.
     *
     * @param updatePasswordDto
     */
    public void completResetPassword(final BaUpdatePasswordDto updatePasswordDto) {
        log.info("Finalise la réinitialisation du mot de passe.");
        final String username = updatePasswordDto.getUsername();
        final String email = updatePasswordDto.getEmail();
        final String telephone = updatePasswordDto.getTelephone();
        final String resetKey = updatePasswordDto.getResetKey();

        Optional<BaUser> oneByEmailAndStatut;
        if (!BaUtils.isEmpty(username)) {
            oneByEmailAndStatut = this.userRepository.findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A);
        } else if (!BaUtils.isEmpty(email)) {
            oneByEmailAndStatut = this.userRepository.findOneByEmailAndStatut(email, EStatut.A);
        } else if (!BaUtils.isEmpty(telephone)) {
            oneByEmailAndStatut = this.userRepository.findOneByTelephoneAndStatut(telephone, EStatut.A);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Votre email ou votre contact est obligatoire");
        }

        if (oneByEmailAndStatut.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations fournies sont incorrectes");
        }

        final BaUser user = oneByEmailAndStatut
                .filter(u -> u.getResetKey() != null
                        && u.getResetDate().isAfter(ZonedDateTime.now().minusHours(Integer.parseInt("2")))
                        && resetKey.equals(u.getResetKey()))
                .map(u -> {
                    u.setResetKey(null);
                    u.setResetDate(null);
                    u.setActivated(Boolean.TRUE);
                    return u;
                }).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le code d'activation a expiré ou est incorrect"));

        if (!Objects.equals(updatePasswordDto.getConfirmer(), updatePasswordDto.getNouveau())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La confirmation ne correspond pas au mot de passe.");
        }
        user.setPassword(this.passwordEncoder.encode(updatePasswordDto.getNouveau()));
        this.userRepository.save(user);
    }

    /**
     * Demander la réinitialisation du mot de passe.
     *
     * @param passwordDto Pour demande la réinitialisation, je prend en compte
     *                    l'email ou le nom d'utilisateur.
     */
    public void requestPasswordReset(final BaUpdatePasswordDto passwordDto) {
        log.info("Demande la réinitialisation de son mot de passe.");
        final String username = passwordDto.getUsername();
        final String email = passwordDto.getEmail();
        final String tel = passwordDto.getTelephone();
        Optional<BaUser> optionalUser;

        if (!BaUtils.isEmpty(username)) {
            optionalUser = this.userRepository.findOneByUsernameIgnoreCaseAndStatut(username, EStatut.A);
        } else if (!BaUtils.isEmpty(email)) {
            optionalUser = this.userRepository.findOneByEmailAndStatut(email, EStatut.A);
        } else if (!BaUtils.isEmpty(tel)) {
            optionalUser = this.userRepository.findOneByTelephoneAndStatut(tel, EStatut.A);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations sont incomplètes");
        }
        if (optionalUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les informations fournies sont incorrectes");
        }

        final BaUser user = optionalUser.get();
        final String resetKey = BaUtils.numberGenerator(Integer.parseInt("8")).toUpperCase();
        user.setResetKey(resetKey.toUpperCase());
        user.setResetDate(ZonedDateTime.now());
        user.setActivated(Boolean.FALSE);
        this.userRepository.save(user);
    }

    /**
     * Authenticate JWT.
     *
     * @param authentication
     * @return un object d'authentification
     */
    public Authentication authenticate(final UsernamePasswordAuthenticationToken authentication) {
        String username = authentication.getName();
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!passwordEncoder.matches(authentication.getCredentials().toString(), userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    /**
     * Activer un utilisateur.
     *
     * @param idUser identifiant de l'utilisateur
     */
    public void activateUser(final String idUser) {
        log.info("Try to activate user : {}", idUser);
        logService.log(new BaLogDto(EAction.D, "Activate un utilisateur " + idUser));

        this.userRepository.findById(idUser)
                .ifPresent(usr -> {
                    usr.setActivated(Boolean.TRUE);
                    userRepository.save(usr);
                });
    }
}
